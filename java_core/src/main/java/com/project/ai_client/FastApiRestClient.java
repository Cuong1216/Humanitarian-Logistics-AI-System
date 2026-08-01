package com.project.ai_client;

import com.google.gson.Gson;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class FastApiRestClient implements IAiClient {

    private static final Logger logger = Logger.getLogger(FastApiRestClient.class.getName());

    private static final int MAX_CACHE_SIZE = 200;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L; // 30 phút

    private record CacheEntry(Object value, long createdAt) {
        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
    }

    private final String baseUrl; // VD: "http://localhost:8000"
    private final HttpClient httpClient;
    private final Gson gson;
    private final CircuitBreaker circuitBreaker;
    private final Map<String, CacheEntry> cache = Collections.synchronizedMap(
        new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > MAX_CACHE_SIZE || eldest.getValue().isExpired();
            }
        }
    );

    public FastApiRestClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.gson = new Gson();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
                
        // Cấu hình Circuit Breaker bằng Java Config (programmatically)
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Ngắt mạch nếu tỷ lệ lỗi >= 50%
                .slidingWindowSize(10) // Dựa trên 10 request gần nhất
                .minimumNumberOfCalls(10) // Tối thiểu 10 request mới bắt đầu tính tỷ lệ lỗi
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Ngắt mạch trong 30 giây
                .recordExceptions(AiApiException.class, Exception.class)
                .build();
                
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        this.circuitBreaker = registry.circuitBreaker("fastApiCircuitBreaker");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R> java.util.concurrent.CompletableFuture<R> executeTask(String endpoint, T requestData, Class<R> returnType) throws Exception {
        // 1. Chuyển DTO đầu vào thành JSON
        String jsonPayload = gson.toJson(requestData);
        
        // Cache Key is a combination of endpoint and the payload
        String cacheKey = endpoint + ":" + jsonPayload;
        if (cache.containsKey(cacheKey)) {
            CacheEntry entry = cache.get(cacheKey);
            if (!entry.isExpired()) {
                return java.util.concurrent.CompletableFuture.completedFuture((R) entry.value());
            }
        }

        // 2. Chuẩn bị Supplier cho HTTP Request
        Supplier<java.util.concurrent.CompletionStage<R>> requestSupplier = () -> {
            String correlationId = UUID.randomUUID().toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .version(HttpClient.Version.HTTP_1_1) 
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .header("X-Correlation-ID", correlationId)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        String responseCorrelationId = response.headers().firstValue("X-Correlation-ID").orElse("N/A");
                        logger.info(String.format("FASTAPI_RESPONSE | endpoint=%s correlation_id=%s status=%d", 
                                endpoint, responseCorrelationId, response.statusCode()));

                        if (response.statusCode() != 200) {
                            throw new AiApiException("Lỗi API FastAPI: " + response.body());
                        }

                        // 3. Parse JSON trả về thành DTO mong muốn
                        R result = gson.fromJson(response.body(), returnType);
                        
                        // Save to cache before returning
                        cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis()));
                        return result;
                    });
        };

        // 4. Áp dụng CircuitBreaker và cơ chế Fallback
        return (java.util.concurrent.CompletableFuture<R>) circuitBreaker.executeCompletionStage(requestSupplier)
                .exceptionally(ex -> {
                    logger.warning("Circuit breaker/Fallback triggered for endpoint " + endpoint + " : " + ex.getMessage());
                    return createFallback(returnType);
                }).toCompletableFuture();
    }
    
    /**
     * Fallback: Trả về AnalysisResult mặc định (Mức độ: UNKNOWN, Khẩn cấp: LOW)
     */
    private <R> R createFallback(Class<R> returnType) {
        try {
            if (returnType.getSimpleName().equals("AnalyzeRes")) {
                // Trả về mặc định Mức độ: UNKNOWN, Khẩn cấp: LOW cho AnalyzeRes
                String fallbackJson = "{\"dominant_emotion\":\"UNKNOWN\",\"humanitarian_signal\":{\"urgency\":\"LOW\",\"is_emergency\":false,\"categories\":[]},\"summary\":\"Fallback: AI Engine Unavailable\"}";
                return gson.fromJson(fallbackJson, returnType);
            } else if (returnType.getSimpleName().equals("AnalysisResult")) {
                // Trả về mặc định Mức độ: UNKNOWN, Khẩn cấp: LOW cho AnalysisResult chung
                R result = returnType.getDeclaredConstructor().newInstance();
                returnType.getMethod("put", String.class, Object.class).invoke(result, "level", "UNKNOWN");
                returnType.getMethod("put", String.class, Object.class).invoke(result, "urgency", "LOW");
                returnType.getMethod("setSummary", String.class).invoke(result, "Fallback: AI Engine Unavailable");
                return result;
            } else if (returnType.getSimpleName().equals("BatchResult")) {
                String fallbackJson = "{\"results\":[]}";
                return gson.fromJson(fallbackJson, returnType);
            }
        } catch (Exception e) {
            logger.severe("Could not create fallback for type: " + returnType.getSimpleName());
        }
        return null;
    }

    public void clearCache() {
        cache.clear();
    }

    public int getCacheSize() {
        return cache.size();
    }
}

package com.project.ai_client;

import com.google.gson.Gson;
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
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R> R executeTask(String endpoint, T requestData, Class<R> returnType) throws Exception {
        // 1. Chuyển DTO đầu vào thành JSON
        String jsonPayload = gson.toJson(requestData);
        
        // Cache Key is a combination of endpoint and the payload
        String cacheKey = endpoint + ":" + jsonPayload;
        if (cache.containsKey(cacheKey)) {
            CacheEntry entry = cache.get(cacheKey);
            if (!entry.isExpired()) {
                return (R) entry.value();
            }
        }

        // 2. Gọi sang FastAPI
        String correlationId = UUID.randomUUID().toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .version(HttpClient.Version.HTTP_1_1) 
                .header("Content-Type", "application/json; charset=utf-8") // Tấm thẻ bài bắt buộc cho FastAPI
                .header("Accept", "application/json")
                .header("X-Correlation-ID", correlationId)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        String responseCorrelationId = response.headers().firstValue("X-Correlation-ID").orElse("N/A");
        logger.info(String.format("FASTAPI_RESPONSE | endpoint=%s correlation_id=%s status=%d", 
                endpoint, responseCorrelationId, response.statusCode()));

        if (response.statusCode() != 200) {
            throw new RuntimeException("Lỗi API FastAPI: " + response.body());
        }

        // 3. Parse JSON trả về thành DTO mong muốn
        R result = gson.fromJson(response.body(), returnType);
        
        // Save to cache before returning
        cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis()));
        return result;
    }
    public void clearCache() {
        cache.clear();
    }

    public int getCacheSize() {
        return cache.size();
    }
}

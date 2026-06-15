package com.project.ai_client;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FastApiRestClient implements IAiClient {

    private final String baseUrl; // VD: "http://localhost:8000"
    private final HttpClient httpClient;
    private final Gson gson;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

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
            return (R) cache.get(cacheKey);
        }

        // 2. Gọi sang FastAPI
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .version(HttpClient.Version.HTTP_1_1) 
                .header("Content-Type", "application/json; charset=utf-8") // Tấm thẻ bài bắt buộc cho FastAPI
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Lỗi API FastAPI: " + response.body());
        }

        // 3. Parse JSON trả về thành DTO mong muốn
        R result = gson.fromJson(response.body(), returnType);
        
        // Save to cache before returning
        cache.put(cacheKey, result);
        return result;
    }
}

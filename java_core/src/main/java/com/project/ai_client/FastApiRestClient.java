package main.java.com.project.ai_client;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class FastApiRestClient implements IAiClient {

    private final String baseUrl; // VD: "http://localhost:8000"
    private final HttpClient httpClient;
    private final Gson gson;

    public FastApiRestClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.gson = new Gson();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public <T, R> R executeTask(String endpoint, T requestData, Class<R> returnType) throws Exception {
        // 1. Chuyển DTO đầu vào thành JSON
        String jsonPayload = gson.toJson(requestData);

        // 2. Gọi sang FastAPI
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Lỗi API FastAPI: " + response.body());
        }

        // 3. Parse JSON trả về thành DTO mong muốn
        return gson.fromJson(response.body(), returnType);
    }
}

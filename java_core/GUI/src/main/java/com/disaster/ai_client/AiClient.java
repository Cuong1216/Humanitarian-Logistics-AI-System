package com.disaster.ai_client;

public class AiClient {
    private String apiUrl;
    private String apiKey;

    public AiClient(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public AIResponseDTO sendRequest(AIRequestDTO request) {
        // Mock AI response
        String mockResult = "Analysis complete for task: " + request.getTask()
            + " on " + request.getPosts().size() + " posts.";
        return new AIResponseDTO(mockResult, 200);
    }

    public String getApiUrl() { return apiUrl; }
    public String getApiKey() { return apiKey; }
}

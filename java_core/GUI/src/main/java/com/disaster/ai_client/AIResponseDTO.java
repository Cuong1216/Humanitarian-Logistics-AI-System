package com.disaster.ai_client;

public class AIResponseDTO {
    private String result;
    private int statusCode;

    public AIResponseDTO() {}
    public AIResponseDTO(String result, int statusCode) {
        this.result = result;
        this.statusCode = statusCode;
    }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
}

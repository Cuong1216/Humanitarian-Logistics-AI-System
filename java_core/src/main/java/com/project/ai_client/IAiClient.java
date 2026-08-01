package com.project.ai_client;

/**
 * Interface cốt lõi. Bất kỳ Client nào (FastAPI, Google Gemini, OpenAI...) 
 * đều phải implement interface này.
 */
public interface IAiClient {
    /**
     * @param endpoint    Đường dẫn API (VD: "/api/sentiment")
     * @param requestData Đối tượng DTO chứa dữ liệu đầu vào
     * @param returnType  Kiểu dữ liệu của DTO đầu ra mong muốn
     * @param <T>         Kiểu của Request
     * @param <R>         Kiểu của Response
     */
    <T, R> java.util.concurrent.CompletableFuture<R> executeTask(String endpoint, T requestData, Class<R> returnType) throws Exception;
}

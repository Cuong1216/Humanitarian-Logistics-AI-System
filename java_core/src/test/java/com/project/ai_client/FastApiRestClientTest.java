package com.project.ai_client;

import com.project.ai_client.dto.AnalyzeReq;
import com.project.ai_client.dto.AnalyzeRes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FastApiRestClientTest {

    @Test
    public void testExecuteTask_CircuitBreakerFallback() throws Exception {
        // 1. Arrange
        FastApiRestClient client = new FastApiRestClient("http://localhost:8000");
        
        // Mock HttpClient để luôn ném Exception (giả lập server sập)
        HttpClient mockHttpClient = mock(HttpClient.class);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection Refused")));

        // Inject mockHttpClient vào class FastApiRestClient qua Reflection
        ReflectionTestUtils.setField(client, "httpClient", mockHttpClient);

        AnalyzeReq.PostData data = new AnalyzeReq.PostData("1", "FB", "Test", "Cứu tôi với", "", "", java.util.Collections.emptyMap(), java.util.Collections.emptyList(), 2);
        AnalyzeReq req = new AnalyzeReq(data);

        // 2. Act
        // Ép CircuitBreaker mở bằng cách gọi liên tục nhiều lần (Cấu hình minimumNumberOfCalls = 10)
        for (int i = 0; i < 15; i++) {
            try {
                client.executeTask("/analyze", req, AnalyzeRes.class).join();
            } catch (Exception e) {
                // Ignore during force open
            }
        }
        
        // Gọi lại 1 lần khi CircuitBreaker chắc chắn đã OPEN
        CompletableFuture<AnalyzeRes> futureResult = client.executeTask("/analyze", req, AnalyzeRes.class);
        AnalyzeRes result = futureResult.join();

        // 3. Assert
        assertNotNull(result, "Fallback mechanism failed to return a default result");
        assertNotNull(result.getHumanitarianSignal(), "Fallback should provide a default humanitarian signal");
        assertEquals("LOW", result.getHumanitarianSignal().getUrgency(), "Urgency should be LOW in fallback");
        assertEquals("UNKNOWN", result.getDominantEmotion(), "Emotion should be UNKNOWN in fallback");
        assertEquals("Fallback: AI Engine Unavailable", result.getSummary(), "Summary should indicate fallback");
    }
}

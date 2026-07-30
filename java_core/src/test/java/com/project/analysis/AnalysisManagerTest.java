package com.project.analysis;
import com.project.ai_client.IAiClient;
import com.project.datacollection.model.SocialMediaPost;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class AnalysisManagerTest {
    private AnalysisManager manager;
    private IAiClient mockClient;

    @BeforeEach
    void setUp() {
        manager = new AnalysisManager();
        mockClient = Mockito.mock(IAiClient.class);
    }

    @Test
    @DisplayName("Manager rỗng — runAll phải trả về list rỗng")
    void testEmptyManagerReturnsEmptyResults() {
        List<AnalysisResult> results = manager.runAll(List.of(), mockClient);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Thêm và xóa analyzer — getAnalyzers() phải phản ánh đúng")
    void testAddRemoveAnalyzer() {
        TaskAnalyzer mockAnalyzer = Mockito.mock(TaskAnalyzer.class);
        manager.addAnalyzer(mockAnalyzer);
        assertEquals(1, manager.getAnalyzers().size());
        manager.removeAnalyzer(mockAnalyzer);
        assertEquals(0, manager.getAnalyzers().size());
    }
}

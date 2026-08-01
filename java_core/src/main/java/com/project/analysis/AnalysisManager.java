package com.project.analysis;

import com.project.ai_client.IAiClient;
import com.project.datacollection.model.SocialMediaPost;
import java.util.ArrayList;
import java.util.List;

public class AnalysisManager {
    private List<TaskAnalyzer> analyzers = new ArrayList<>();

    public void addAnalyzer(TaskAnalyzer analyzer) {
        analyzers.add(analyzer);
    }

    public void removeAnalyzer(TaskAnalyzer analyzer) {
        analyzers.remove(analyzer);
    }

    public List<AnalysisResult> runAll(List<SocialMediaPost> posts, IAiClient aiClient) {
        List<java.util.concurrent.CompletableFuture<AnalysisResult>> futures = new ArrayList<>();
        
        for (TaskAnalyzer analyzer : analyzers) {
            java.util.concurrent.CompletableFuture<AnalysisResult> future = analyzer.analyze(posts, aiClient)
                .exceptionally(ex -> {
                    System.err.println("Lỗi khi chạy analyzer " + analyzer.getClass().getSimpleName() + ": " + ex.getMessage());
                    return null; // Handle error gracefully without breaking others
                });
            futures.add(future);
        }
        
        // Chờ tất cả hoàn thành song song
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        
        List<AnalysisResult> results = new ArrayList<>();
        for (java.util.concurrent.CompletableFuture<AnalysisResult> future : futures) {
            AnalysisResult res = future.join();
            if (res != null) {
                results.add(res);
            }
        }
        return results;
    }

    public List<TaskAnalyzer> getAnalyzers() { return analyzers; }
}

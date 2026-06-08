package com.disaster.analysis;

import com.disaster.ai_client.AiClient;
import com.disaster.datacollection.model.SocialMediaPost;
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

    public List<AnalysisResult> runAll(List<SocialMediaPost> posts, AiClient aiClient) {
        List<AnalysisResult> results = new ArrayList<>();
        for (TaskAnalyzer analyzer : analyzers) {
            results.add(analyzer.analyze(posts, aiClient));
        }
        return results;
    }

    public List<TaskAnalyzer> getAnalyzers() { return analyzers; }
}

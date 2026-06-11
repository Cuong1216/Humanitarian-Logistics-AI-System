package main.java.com.project.analysis;

import main.java.com.project.ai_client.IAiClient;
import main.java.com.project.datacollection.model.SocialMediaPost;
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
        List<AnalysisResult> results = new ArrayList<>();
        for (TaskAnalyzer analyzer : analyzers) {
            results.add(analyzer.analyze(posts, aiClient));
        }
        return results;
    }

    public List<TaskAnalyzer> getAnalyzers() { return analyzers; }
}

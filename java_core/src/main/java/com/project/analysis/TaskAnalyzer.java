package com.project.analysis;

import java.util.List;

public interface TaskAnalyzer {
    void analyze(String text, AnalysisResult result);

    default AnalysisResult analyze(List<SocialMediaPost> posts, AiClient aiClient) {
        return null;
    }
}

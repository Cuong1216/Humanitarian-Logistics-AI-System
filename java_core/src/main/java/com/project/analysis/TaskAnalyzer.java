package com.project.analysis;

import com.project.ai_client.IAiClient;
import com.project.datacollection.model.SocialMediaPost;
import java.util.List;

public interface TaskAnalyzer {
    default AnalysisResult analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        return null;
    }
}

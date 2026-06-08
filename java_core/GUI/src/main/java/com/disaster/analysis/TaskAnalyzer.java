package com.disaster.analysis;

import com.disaster.ai_client.AiClient;
import com.disaster.datacollection.model.SocialMediaPost;
import java.util.List;

public interface TaskAnalyzer {
    AnalysisResult analyze(List<SocialMediaPost> posts, AiClient aiClient);
}

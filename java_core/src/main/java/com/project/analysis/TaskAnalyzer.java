package main.java.com.project.analysis;

import main.java.com.project.ai_client.IAiClient;
import main.java.com.project.datacollection.model.SocialMediaPost;
import java.util.List;

public interface TaskAnalyzer {
    AnalysisResult analyze(List<SocialMediaPost> posts, IAiClient aiClient);
}

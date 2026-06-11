package main.java.com.project.analysis;

import main.java.com.project.ai_client.IAiClient;
import main.java.com.project.ai_client.dto.AnalyzeReq;
import main.java.com.project.ai_client.dto.AnalyzeRes;
import main.java.com.project.datacollection.model.SocialMediaPost;
import java.util.*;

public class SentimentAnalyzeOverTime implements TaskAnalyzer {

    @Override
    public AnalysisResult analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        AnalysisResult result = new AnalysisResult("SentimentOverTime");
        
        Map<String, Double> negativeScoreMap = new LinkedHashMap<>();
        Map<String, String> emotionMap = new LinkedHashMap<>();
        
        for (SocialMediaPost post : posts) {
            if (aiClient != null) {
                try {
                    AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                        post.getId() != null ? post.getId() : UUID.randomUUID().toString(),
                        "facebook", "unknown", post.getContent(), "", "", 
                        new HashMap<>(), new ArrayList<>(), 0
                    );
                    AnalyzeRes res = aiClient.executeTask("/analyze", new AnalyzeReq(postData), AnalyzeRes.class);
                    
                    if (res != null) {
                        negativeScoreMap.put(post.getId(), res.getNegativeScore());
                        emotionMap.put(post.getId(), res.getDominantEmotion());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        result.put("negativeScoreTimeline", negativeScoreMap);
        result.put("dominantEmotions", emotionMap);
        result.setSummary("Sentiment & Negative Score analyzed over " + posts.size() + " posts using AI Engine.");
        return result;
    }
}

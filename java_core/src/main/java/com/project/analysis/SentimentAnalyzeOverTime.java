package com.project.analysis;

import com.project.ai_client.IAiClient;
import com.project.ai_client.dto.AnalyzeReq;
import com.project.ai_client.dto.AnalyzeRes;
import com.project.datacollection.model.SocialMediaPost;
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
                    String preprocessedText = WordPreprocessor.preprocess(post.getContent());
                    AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                        post.getId() != null ? post.getId() : java.util.UUID.randomUUID().toString(),
                        post.getPlatform() != null ? post.getPlatform().toLowerCase() : "facebook",
                        post.getAuthor() != null ? post.getAuthor() : "unknown",
                        preprocessedText,
                        "",
                        "",
                        post.getReactions(),
                        post.getComments(),
                        post.getShareCount()
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

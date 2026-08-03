package com.humanitarian.logistics.core.analysis;

import com.humanitarian.logistics.core.ai_client.IAiClient;
import com.humanitarian.logistics.core.ai_client.dto.AnalyzeReq;
import com.humanitarian.logistics.core.ai_client.dto.AnalyzeRes;
import com.humanitarian.logistics.core.entity.SocialMediaPost;
import java.util.*;

public class SentimentAnalyzeOverTime implements TaskAnalyzer {

    @Override
    public java.util.concurrent.CompletableFuture<AnalysisResult> analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        AnalysisResult result = new AnalysisResult("SentimentOverTime");
        
        Map<String, Double> negativeScoreMap = java.util.Collections.synchronizedMap(new LinkedHashMap<>());
        Map<String, String> emotionMap = java.util.Collections.synchronizedMap(new LinkedHashMap<>());
        
        if (aiClient == null) {
            result.put("negativeScoreTimeline", negativeScoreMap);
            result.put("dominantEmotions", emotionMap);
            result.setSummary("Sentiment & Negative Score analyzed over " + posts.size() + " posts using AI Engine.");
            return java.util.concurrent.CompletableFuture.completedFuture(result);
        }

        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
        for (SocialMediaPost post : posts) {
            try {
                String preprocessedText = WordPreprocessor.preprocess(post.getContent());
                AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                    post.getId() != null ? post.getId().toString() : java.util.UUID.randomUUID().toString(),
                    post.getSource() != null ? post.getSource().toLowerCase() : "facebook",
                    post.getAuthor() != null ? post.getAuthor() : "unknown",
                    preprocessedText,
                    "",
                    "",
                    new java.util.HashMap<>(),
                    new java.util.ArrayList<>(),
                    0
                );
                
                java.util.concurrent.CompletableFuture<Void> future = aiClient.executeTask("/analyze", new AnalyzeReq(postData), AnalyzeRes.class)
                    .thenAccept(res -> {
                        if (res != null) {
                            String postId = post.getId() != null ? post.getId().toString() : "unknown";
                            negativeScoreMap.put(postId, res.getNegativeScore());
                            emotionMap.put(postId, res.getDominantEmotion());
                        }
                    }).exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
                futures.add(future);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
            .thenApply(v -> {
                result.put("negativeScoreTimeline", negativeScoreMap);
                result.put("dominantEmotions", emotionMap);
                result.setSummary("Sentiment & Negative Score analyzed over " + posts.size() + " posts using AI Engine.");
                return result;
            });
    }
}

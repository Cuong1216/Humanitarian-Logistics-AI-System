package com.humanitarian.logistics.core.analysis;

import com.humanitarian.logistics.core.ai_client.IAiClient;
import com.humanitarian.logistics.core.ai_client.dto.AnalyzeReq;
import com.humanitarian.logistics.core.ai_client.dto.AnalyzeRes;
import com.humanitarian.logistics.core.entity.SocialMediaPost;
import java.util.*;

public class ReliefSentimentAnalyzer implements TaskAnalyzer {
    private List<String> supportItem;

    public ReliefSentimentAnalyzer() {
        this.supportItem = Arrays.asList("food", "water", "medical", "shelter", "rescue");
    }

    public List<String> getSupportItem() { return supportItem; }
    public void setSupportItem(List<String> supportItem) { this.supportItem = supportItem; }

    @Override
    public java.util.concurrent.CompletableFuture<AnalysisResult> analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        AnalysisResult result = new AnalysisResult("ReliefSentimentAnalyzer");
        Map<String, Integer> itemDemand = java.util.Collections.synchronizedMap(new LinkedHashMap<>());
        for (String item : supportItem) itemDemand.put(item, 0);

        if (aiClient == null) {
            result.put("itemDemand", itemDemand);
            result.setSummary("Relief demand analyzed across " + posts.size() + " posts using AI Engine.");
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
                        if (res != null && res.getHumanitarianSignal() != null) {
                            List<String> categories = res.getHumanitarianSignal().getCategories();
                            if (categories != null) {
                                synchronized(itemDemand) {
                                    for (String cat : categories) {
                                        String key = cat.toLowerCase();
                                        if (itemDemand.containsKey(key)) {
                                            itemDemand.put(key, itemDemand.get(key) + 1);
                                        }
                                    }
                                }
                            }
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
                result.put("itemDemand", itemDemand);
                result.setSummary("Relief demand analyzed across " + posts.size() + " posts using AI Engine.");
                return result;
            });
    }
}

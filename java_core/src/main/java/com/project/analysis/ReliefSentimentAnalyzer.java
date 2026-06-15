package com.project.analysis;

import com.project.ai_client.IAiClient;
import com.project.ai_client.dto.AnalyzeReq;
import com.project.ai_client.dto.AnalyzeRes;
import com.project.datacollection.model.SocialMediaPost;
import java.util.*;

public class ReliefSentimentAnalyzer implements TaskAnalyzer {
    private List<String> supportItem;

    public ReliefSentimentAnalyzer() {
        this.supportItem = Arrays.asList("food", "water", "medical", "shelter", "rescue");
    }

    public List<String> getSupportItem() { return supportItem; }
    public void setSupportItem(List<String> supportItem) { this.supportItem = supportItem; }

    @Override
    public AnalysisResult analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        AnalysisResult result = new AnalysisResult("ReliefSentimentAnalyzer");
        Map<String, Integer> itemDemand = new LinkedHashMap<>();
        for (String item : supportItem) itemDemand.put(item, 0);

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
                    
                    if (res != null && res.getHumanitarianSignal() != null) {
                        List<String> categories = res.getHumanitarianSignal().getCategories();
                        if (categories != null) {
                            for (String cat : categories) {
                                String key = cat.toLowerCase();
                                if (itemDemand.containsKey(key)) {
                                    itemDemand.put(key, itemDemand.get(key) + 1);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        result.put("itemDemand", itemDemand);
        result.setSummary("Relief demand analyzed across " + posts.size() + " posts using AI Engine.");
        return result;
    }
}

package com.project.analysis;

import com.project.ai_client.IAiClient;
import com.project.ai_client.dto.AnalyzeReq;
import com.project.ai_client.dto.AnalyzeRes;
import com.project.datacollection.model.SocialMediaPost;
import java.util.*;

public class DamageCategorizer implements TaskAnalyzer {
    private List<String> urgencyLevels;

    public DamageCategorizer() {
        this.urgencyLevels = Arrays.asList("low", "medium", "high", "critical");
    }

    public List<String> getUrgencyLevels() { return urgencyLevels; }
    public void setUrgencyLevels(List<String> urgencyLevels) {
        this.urgencyLevels = urgencyLevels;
    }

    @Override
    public AnalysisResult analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        AnalysisResult result = new AnalysisResult("UrgencyCategorizer");
        Map<String, Integer> urgencyCount = new LinkedHashMap<>();
        for (String lvl : urgencyLevels) urgencyCount.put(lvl, 0);

        for (SocialMediaPost post : posts) {
            if (aiClient != null) {
                try {
                    AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                        post.getId() != null ? post.getId() : java.util.UUID.randomUUID().toString(),
                        post.getPlatform() != null ? post.getPlatform().toLowerCase() : "facebook",
                        post.getAuthor() != null ? post.getAuthor() : "unknown",
                        post.getContent(),
                        "",
                        "",
                        post.getReactions(),
                        post.getComments(),
                        post.getShareCount()
                    );
                    AnalyzeRes res = aiClient.executeTask("/analyze", new AnalyzeReq(postData), AnalyzeRes.class);
                    
                    if (res != null && res.getHumanitarianSignal() != null) {
                        String urgency = res.getHumanitarianSignal().getUrgency();
                        if (urgency != null && urgencyCount.containsKey(urgency.toLowerCase())) {
                            String key = urgency.toLowerCase();
                            urgencyCount.put(key, urgencyCount.get(key) + 1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        result.put("urgencyCount", urgencyCount);
        result.setSummary("Categorized urgency of " + posts.size() + " posts using AI Engine.");
        return result;
    }
}

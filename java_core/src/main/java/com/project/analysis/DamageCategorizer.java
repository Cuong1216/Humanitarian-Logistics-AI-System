package main.java.com.project.analysis;

import main.java.com.project.ai_client.IAiClient;
import main.java.com.project.ai_client.dto.AnalyzeReq;
import main.java.com.project.ai_client.dto.AnalyzeRes;
import main.java.com.project.datacollection.model.SocialMediaPost;
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
                        post.getId() != null ? post.getId() : UUID.randomUUID().toString(),
                        "facebook", "unknown", post.getContent(), "", "", 
                        new HashMap<>(), new ArrayList<>(), 0
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

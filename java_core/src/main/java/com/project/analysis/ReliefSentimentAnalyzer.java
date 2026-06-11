package main.java.com.project.analysis;

import main.java.com.project.ai_client.IAiClient;
import main.java.com.project.ai_client.dto.AnalyzeReq;
import main.java.com.project.ai_client.dto.AnalyzeRes;
import main.java.com.project.datacollection.model.SocialMediaPost;
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
                    AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                        post.getId() != null ? post.getId() : UUID.randomUUID().toString(),
                        "facebook", "unknown", post.getContent(), "", "", 
                        new HashMap<>(), new ArrayList<>(), 0
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

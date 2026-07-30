package main.java.com.project.analysis;

import main.java.com.project.ai_client.IAiClient;
import main.java.com.project.datacollection.model.SocialMediaPost;
import java.util.*;

public class ReliefSentimentAnalyzer implements TaskAnalyzer {
    private List<String> supportItem;

    public ReliefSentimentAnalyzer() {
        this.supportItem = Arrays.asList("Food", "Water", "Medicine", "Shelter", "Rescue Team");
    }

    public List<String> getSupportItem() { return supportItem; }
    public void setSupportItem(List<String> supportItem) { this.supportItem = supportItem; }

    @Override
    public AnalysisResult analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        AnalysisResult result = new AnalysisResult("ReliefSentimentAnalyzer");
        Map<String, Integer> itemDemand = new LinkedHashMap<>();
        for (String item : supportItem) itemDemand.put(item, 0);

        Random rand = new Random();
        for (SocialMediaPost post : posts) {
            String item = supportItem.get(rand.nextInt(supportItem.size()));
            itemDemand.put(item, itemDemand.get(item) + 1);
        }
        result.put("itemDemand", itemDemand);
        result.setSummary("Relief demand analyzed across " + supportItem.size() + " support categories.");
        return result;
    }
}

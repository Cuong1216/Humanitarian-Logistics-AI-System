package com.disaster.analysis;

import com.disaster.ai_client.AiClient;
import com.disaster.datacollection.model.SocialMediaPost;
import java.util.*;

public class DamageCategorizer implements TaskAnalyzer {
    private List<String> damageCategories;

    public DamageCategorizer() {
        this.damageCategories = Arrays.asList(
            "Flood", "Building Collapse", "Road Damage", "Power Outage", "Fire"
        );
    }

    public List<String> getDamageCategories() { return damageCategories; }
    public void setDamageCategories(List<String> damageCategories) {
        this.damageCategories = damageCategories;
    }

    @Override
    public AnalysisResult analyze(List<SocialMediaPost> posts, AiClient aiClient) {
        AnalysisResult result = new AnalysisResult("DamageCategorizer");
        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        for (String cat : damageCategories) categoryCount.put(cat, 0);

        Random rand = new Random();
        for (SocialMediaPost post : posts) {
            String cat = damageCategories.get(rand.nextInt(damageCategories.size()));
            categoryCount.put(cat, categoryCount.get(cat) + 1);
        }
        result.put("categoryCount", categoryCount);
        result.setSummary("Categorized " + posts.size() + " posts into " + damageCategories.size() + " damage types.");
        return result;
    }
}

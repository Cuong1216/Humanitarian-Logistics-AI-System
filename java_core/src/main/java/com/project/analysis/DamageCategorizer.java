package main.java.com.project.analysis;

import main.java.com.project.ai_client.IAiClient;
import main.java.com.project.datacollection.model.SocialMediaPost;

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

    // DTOs for mapping AI response
    public static class BatchRequest {
        public List<SocialMediaPost> posts;
        public BatchRequest(List<SocialMediaPost> posts) {
            this.posts = posts;
        }
    }

    public static class BatchResult {
        public List<AnalysisResultItem> results;
    }

    public static class AnalysisResultItem {
        public HumanitarianSignal humanitarian_signal;
    }

    public static class HumanitarianSignal {
        public List<String> categories;
    }

    @Override
    public AnalysisResult analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        AnalysisResult result = new AnalysisResult("DamageCategorizer");
        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        for (String cat : damageCategories) {
            categoryCount.put(cat, 0);
        }

        boolean useAi = false;
        
        // Phương án A: Dùng AI nếu có aiClient
        if (aiClient != null) {
            try {
                BatchRequest req = new BatchRequest(posts);
                BatchResult aiResponse = aiClient.executeTask("/analyze/batch", req, BatchResult.class);
                if (aiResponse != null && aiResponse.results != null) {
                    for (AnalysisResultItem item : aiResponse.results) {
                        if (item.humanitarian_signal != null && item.humanitarian_signal.categories != null) {
                            for (String cat : item.humanitarian_signal.categories) {
                                String mappedCat = mapAiCategory(cat);
                                if (mappedCat != null && categoryCount.containsKey(mappedCat)) {
                                    categoryCount.put(mappedCat, categoryCount.get(mappedCat) + 1);
                                }
                            }
                        }
                    }
                    useAi = true;
                }
            } catch (Exception e) {
                System.err.println("AI classification failed, falling back to keywords: " + e.getMessage());
            }
        }

        // Phương án B: Keyword fallback nếu không gọi được AI
        if (!useAi) {
            Map<String, List<String>> keywords = new HashMap<>();
            keywords.put("Flood", Arrays.asList("lũ", "ngập", "nước dâng", "flood", "inundation"));
            keywords.put("Building Collapse", Arrays.asList("sập", "đổ", "vỡ", "collapse", "nhà sập"));
            keywords.put("Road Damage", Arrays.asList("đường hư", "cầu sập", "tắc đường", "road blocked"));
            keywords.put("Power Outage", Arrays.asList("mất điện", "cúp điện", "power out"));
            keywords.put("Fire", Arrays.asList("cháy", "hỏa hoạn", "fire"));

            for (SocialMediaPost post : posts) {
                String content = post.getContent();
                if (content == null) continue;
                String lowerContent = content.toLowerCase();

                for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
                    String category = entry.getKey();
                    for (String kw : entry.getValue()) {
                        if (lowerContent.contains(kw.toLowerCase())) {
                            categoryCount.put(category, categoryCount.get(category) + 1);
                            break; // Tính 1 lần cho 1 category trên 1 post
                        }
                    }
                }
            }
        }

        result.put("categoryCount", categoryCount);
        result.setSummary("Categorized " + posts.size() + " posts into damage types using " + (useAi ? "AI" : "Keyword Fallback") + ".");
        return result;
    }

    private String mapAiCategory(String aiCat) {
        if (aiCat == null) return null;
        switch (aiCat.toLowerCase()) {
            case "water":
                return "Flood";
            case "shelter":
                return "Building Collapse";
            case "transport":
                return "Road Damage";
            default:
                return null;
        }
    }
}

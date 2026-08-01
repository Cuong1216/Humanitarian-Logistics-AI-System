package com.project.analysis;

import com.project.ai_client.IAiClient;
import com.project.datacollection.model.SocialMediaPost;
import java.util.*;

public class DamageCategorizer implements TaskAnalyzer {
    private List<String> damageCategories;

    public DamageCategorizer() {
        this.damageCategories = Arrays.asList(
            "Người bị ảnh hưởng",
            "Gián đoạn các hoạt động kinh tế sản xuất",
            "Nhà cửa hoặc tòa nhà bị hư hỏng",
            "Tài sản cá nhân bị mất",
            "Cơ sở hạ tầng bị hư hỏng",
            "Nông nghiệp & Vật nuôi bị thiệt hại"
        );
    }

    public List<String> getDamageCategories() {
        return damageCategories;
    }

    public static class ClassificationResult {
        public String category;
        public String evidence;

        public ClassificationResult(String category, String evidence) {
            this.category = category;
            this.evidence = evidence;
        }
    }

    public ClassificationResult classifyPost(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ClassificationResult("Khác (Tin tức chung / Chưa phân loại)", "Không có nội dung");
        }

        String lowerText = content.toLowerCase();

        // 1. Keywords for Human Impact
        List<String> humanKeywords = Arrays.asList(
            "chết", "thiệt mạng", "mất tích", "thi thể", "xác", "quan tài", "bị thương", 
            "cấp cứu", "thương vong", "chấn thương", "người bị nạn", "tính mạng", "đuối nước", 
            "cuốn trôi mất", "tìm xác", "hóa vàng", "tang thương", "nạn nhân"
        );

        // 2. Keywords for Economic/Production Disruption
        List<String> economicKeywords = Arrays.asList(
            "mất điện", "cúp điện", "mất nước", "mất mạng", "mất sóng", "ngừng hoạt động", 
            "đóng cửa", "đình trệ", "nghỉ làm", "gián đoạn", "không bán hàng", "tăng giá", 
            "thiếu lương thực", "đắt đỏ", "cạn kiệt", "chợ đóng cửa", "cửa hàng ngừng", 
            "giá tăng", "thực phẩm khan hiếm"
        );

        // 3. Keywords for Damaged Houses/Buildings
        List<String> houseKeywords = Arrays.asList(
            "sập nhà", "tốc mái", "mái tôn", "đổ tường", "bay mái", "bay nóc", "nhà đổ", 
            "nứt nhà", "ngập nóc", "ngập mái", "sập trần", "sập tường", "bay mái tôn", 
            "hỏng mái", "nhà sập", "sập tiệm", "tòa nhà đổ", "nhà bay"
        );

        // 4. Keywords for Lost Personal Property
        List<String> propertyKeywords = Arrays.asList(
            "hỏng xe", "ngập xe", "trôi xe", "chìm xuồng", "trôi đồ", "ướt hết", "ti vi", 
            "tủ lạnh", "máy giặt", "xe máy", "ô tô", "mất đồ", "hỏng tài sản", "trôi mất xe", 
            "hỏng đồ đạc", "đồ dùng gia đình", "ướt sạch đồ"
        );

        // 5. Keywords for Damaged Infrastructure
        List<String> infraKeywords = Arrays.asList(
            "sạt lở", "sập cầu", "sập đường", "đập tràn", "đê", "đê điều", "đường ngập", 
            "gãy cây", "cây đổ", "cột điện", "sụt lún", "cầu gãy", "cô lập", "chia cắt", 
            "quốc lộ", "cầu sập", "sạt lở núi", "vỡ đê", "cột điện đổ", "tắc đường"
        );

        // 6. Keywords for Agricultural/Livestock Damage
        List<String> agriKeywords = Arrays.asList(
            "ngập lúa", "mất mùa", "chết gà", "chết lợn", "chết bò", "rau màu", "ao cá", 
            "vườn tược", "trôi ao", "hoa màu", "cây ăn quả", "lúa ngập", "đàn lợn", "đàn gà", 
            "gia súc", "gia cầm", "phù sa bồi lấp", "hỏng lúa", "trôi ao cá", "trang trại",
            "chết vịt", "chết gia súc", "thiệt hại lúa", "nông nghiệp"
        );

        // Match counts and matching words lists
        int humanMatches = 0; List<String> matchedHuman = new ArrayList<>();
        int economicMatches = 0; List<String> matchedEconomic = new ArrayList<>();
        int houseMatches = 0; List<String> matchedHouse = new ArrayList<>();
        int propertyMatches = 0; List<String> matchedProperty = new ArrayList<>();
        int infraMatches = 0; List<String> matchedInfra = new ArrayList<>();
        int agriMatches = 0; List<String> matchedAgri = new ArrayList<>();

        for (String kw : humanKeywords) {
            if (lowerText.contains(kw)) {
                humanMatches++;
                matchedHuman.add(kw);
            }
        }
        for (String kw : economicKeywords) {
            if (lowerText.contains(kw)) {
                economicMatches++;
                matchedEconomic.add(kw);
            }
        }
        for (String kw : houseKeywords) {
            if (lowerText.contains(kw)) {
                houseMatches++;
                matchedHouse.add(kw);
            }
        }
        for (String kw : propertyKeywords) {
            if (lowerText.contains(kw)) {
                propertyMatches++;
                matchedProperty.add(kw);
            }
        }
        for (String kw : infraKeywords) {
            if (lowerText.contains(kw)) {
                infraMatches++;
                matchedInfra.add(kw);
            }
        }
        for (String kw : agriKeywords) {
            if (lowerText.contains(kw)) {
                agriMatches++;
                matchedAgri.add(kw);
            }
        }

        // Determine category with max matches
        int max = 0;
        String selectedCategory = "Khác (Tin tức chung / Chưa phân loại)";
        String selectedEvidence = "Không phát hiện từ khóa đặc trưng";

        if (humanMatches > max) {
            max = humanMatches;
            selectedCategory = "Người bị ảnh hưởng";
            selectedEvidence = String.join(", ", matchedHuman);
        }
        if (houseMatches > max) {
            max = houseMatches;
            selectedCategory = "Nhà cửa hoặc tòa nhà bị hư hỏng";
            selectedEvidence = String.join(", ", matchedHouse);
        }
        if (infraMatches > max) {
            max = infraMatches;
            selectedCategory = "Cơ sở hạ tầng bị hư hỏng";
            selectedEvidence = String.join(", ", matchedInfra);
        }
        if (agriMatches > max) {
            max = agriMatches;
            selectedCategory = "Nông nghiệp & Vật nuôi bị thiệt hại";
            selectedEvidence = String.join(", ", matchedAgri);
        }
        if (economicMatches > max) {
            max = economicMatches;
            selectedCategory = "Gián đoạn các hoạt động kinh tế sản xuất";
            selectedEvidence = String.join(", ", matchedEconomic);
        }
        if (propertyMatches > max) {
            max = propertyMatches;
            selectedCategory = "Tài sản cá nhân bị mất";
            selectedEvidence = String.join(", ", matchedProperty);
        }

        return new ClassificationResult(selectedCategory, selectedEvidence);
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
    public java.util.concurrent.CompletableFuture<AnalysisResult> analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        AnalysisResult result = new AnalysisResult("DamageCategorizer");
        Map<String, Integer> categoryCount = java.util.Collections.synchronizedMap(new LinkedHashMap<>());
        for (String cat : damageCategories) {
            categoryCount.put(cat, 0);
        }

        if (aiClient != null) {
            BatchRequest req = new BatchRequest(posts);
            try {
                return aiClient.executeTask("/analyze/batch", req, BatchResult.class)
                    .handle((aiResponse, ex) -> {
                        boolean useAi = false;
                        if (ex == null && aiResponse != null && aiResponse.results != null) {
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
                        } else if (ex != null) {
                            System.err.println("AI classification failed, falling back to keywords: " + ex.getMessage());
                        }
                        
                        if (!useAi) {
                            fallbackToKeywords(posts, categoryCount);
                        }
                        
                        result.put("categoryCount", categoryCount);
                        result.put("damageCounts", categoryCount);
                        result.setSummary("Categorized " + posts.size() + " posts into damage types using " + (useAi ? "AI" : "Keyword Fallback") + ".");
                        return result;
                    });
            } catch (Exception e) {
                System.err.println("AI classification failed, falling back to keywords: " + e.getMessage());
                fallbackToKeywords(posts, categoryCount);
                result.put("categoryCount", categoryCount);
                result.put("damageCounts", categoryCount);
                result.setSummary("Categorized " + posts.size() + " posts into damage types using Keyword Fallback.");
                return java.util.concurrent.CompletableFuture.completedFuture(result);
            }
        } else {
            fallbackToKeywords(posts, categoryCount);
            result.put("categoryCount", categoryCount);
            result.put("damageCounts", categoryCount);
            result.setSummary("Categorized " + posts.size() + " posts into damage types using Keyword Fallback.");
            return java.util.concurrent.CompletableFuture.completedFuture(result);
        }
    }

    private void fallbackToKeywords(List<SocialMediaPost> posts, Map<String, Integer> categoryCount) {
        Map<String, List<String>> keywords = new HashMap<>();
        keywords.put("Người bị ảnh hưởng", Arrays.asList("chết", "bị thương", "mất tích", "nạn nhân"));
        keywords.put("Gián đoạn các hoạt động kinh tế sản xuất", Arrays.asList("mất điện", "cúp điện", "đóng cửa", "đình trệ"));
        keywords.put("Nhà cửa hoặc tòa nhà bị hư hỏng", Arrays.asList("sập", "tốc mái", "nhà đổ", "vỡ"));
        keywords.put("Tài sản cá nhân bị mất", Arrays.asList("trôi xe", "hỏng đồ", "mất tài sản"));
        keywords.put("Cơ sở hạ tầng bị hư hỏng", Arrays.asList("đường hư", "cầu sập", "tắc đường", "sạt lở"));
        keywords.put("Nông nghiệp & Vật nuôi bị thiệt hại", Arrays.asList("ngập lúa", "chết gà", "chết lợn", "mất mùa"));

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

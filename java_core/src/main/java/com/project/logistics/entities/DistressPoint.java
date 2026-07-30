package com.project.logistics.entities;

import java.util.List;

public class DistressPoint extends Location {
    private String postId;
    private String urgency; // "critical", "high", "medium", "low"
    private List<String> requiredSupplies;
    private int affectedPeople;
    private String status; // "PENDING", "DISPATCHED"
    private String recommendedAction;

    public DistressPoint() {
        this.status = "PENDING";
    }

    public DistressPoint(double lat, double lon, String address, String postId, String urgency, List<String> requiredSupplies, int affectedPeople, String recommendedAction) {
        super(lat, lon, address);
        this.postId = postId;
        this.urgency = urgency;
        this.requiredSupplies = requiredSupplies;
        this.affectedPeople = affectedPeople;
        this.recommendedAction = recommendedAction;
        this.status = "PENDING";
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public List<String> getRequiredSupplies() { return requiredSupplies; }
    public void setRequiredSupplies(List<String> requiredSupplies) { this.requiredSupplies = requiredSupplies; }
    public int getAffectedPeople() { return affectedPeople; }
    public void setAffectedPeople(int affectedPeople) { this.affectedPeople = affectedPeople; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    // Helper for table display of supplies
    public String getSuppliesString() {
        if (requiredSupplies == null || requiredSupplies.isEmpty()) return "Không có";
        java.util.List<String> translated = new java.util.ArrayList<>();
        for (String s : requiredSupplies) {
            switch(s.toLowerCase().trim()) {
                case "food": translated.add("Lương thực"); break;
                case "water": translated.add("Nước sạch"); break;
                case "medical": translated.add("Y tế/Thuốc men"); break;
                case "shelter": translated.add("Nhà ở/Bạt che"); break;
                case "rescue": translated.add("Cứu hộ/Áo phao"); break;
                case "transport": translated.add("Vận chuyển"); break;
                case "sanitation": translated.add("Vệ sinh"); break;
                default: translated.add(s);
            }
        }
        return String.join(", ", translated);
    }
}

package main.java.com.project.ai_client.dto; 

import java.util.List;

public class AnalyzeRes {
    private String post_id;
    private String keyword;
    private String dominant_emotion;
    private double negative_score;
    private double confidence;
    private HumanitarianSignal humanitarian_signal; // Object lồng
    private String summary;
    private String source;

    // Getters cho các bài toán thống kê
    public String getPostId() { return post_id; }
    public String getKeyword() { return keyword; }
    public String getDominantEmotion() { return dominant_emotion; }
    public double getNegativeScore() { return negative_score; }
    public HumanitarianSignal getHumanitarianSignal() { return humanitarian_signal; }
    public String getSummary() { return summary; }

    // Lớp nội bộ ánh xạ cụ thể tín hiệu nhân đạo
    public static class HumanitarianSignal {
        private boolean is_emergency;
        private String urgency;
        private List<String> categories; // [food, water, medical, rescue, transport]
        private List<String> locations;
        private int affected_people_estimate;
        private String recommended_action;

        // Getters
        public boolean isEmergency() { return is_emergency; }
        public String getUrgency() { return urgency; }
        public List<String> getCategories() { return categories; }
        public List<String> getLocations() { return locations; }
        public int getAffectedPeopleEstimate() { return affected_people_estimate; }
        public String getRecommendedAction() { return recommended_action; }
    }

	public String getPost_id() {
		return post_id;
	}
	public String getDominant_emotion() {
		return dominant_emotion;
	}
	public double getNegative_score() {
		return negative_score;
	}
	public double getConfidence() {
		return confidence;
	}
	public HumanitarianSignal getHumanitarian_signal() {
		return humanitarian_signal;
	}
	public String getSource() {
		return source;
	}
}

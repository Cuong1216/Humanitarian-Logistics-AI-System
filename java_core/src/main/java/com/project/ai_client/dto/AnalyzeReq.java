package main.java.com.project.ai_client.dto; 

import java.util.List;
import java.util.Map;

public class AnalyzeReq {
    private PostData post;

    public AnalyzeReq(PostData post) {
        this.post = post;
    }

    public PostData getPost() { return post; }

    // Đối tượng Post chi tiết bên trong JSON
    public static class PostData {
        private String id;
        private String platform;
        private String author;
        private String text;
        private String keyword;
        private String location_hint;
        private Map<String, Integer> reactions; // Dùng Map để nhận các trường sad, angry, like... linh hoạt
        private List<String> comments;
        private int shares;

        // Constructor đầy đủ để tầng Analysis dễ dàng đóng gói dữ liệu
        public PostData(String id, String platform, String author, String text, String keyword, String locationHint, Map<String, Integer> reactions, List<String> comments, int shares) {
            this.id = id;
            this.platform = platform;
            this.author = author;
            this.text = text;
            this.keyword = keyword;
            this.location_hint = locationHint;
            this.reactions = reactions;
            this.comments = comments;
            this.shares = shares;
        }

        // Getters
        public String getId() { return id; }
        public String getText() { return text; }
        public String getKeyword() { return keyword; }

		public String getPlatform() {
			return platform;
		}

		public String getAuthor() {
			return author;
		}

		public String getLocation_hint() {
			return location_hint;
		}

		public Map<String, Integer> getReactions() {
			return reactions;
		}

		public List<String> getComments() {
			return comments;
		}

		public int getShares() {
			return shares;
		}
    }
}

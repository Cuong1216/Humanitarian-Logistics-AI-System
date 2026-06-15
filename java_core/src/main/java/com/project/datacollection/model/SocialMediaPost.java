package com.project.datacollection.model;
import java.time.LocalDateTime;

public class SocialMediaPost {
    private String id;
    private String content;
    private String author;
    private LocalDateTime timestamp;
    private String platform;
    private int likeCount;
    private int shareCount;
    private java.util.List<String> comments = new java.util.ArrayList<>();
    private java.util.Map<String, Integer> reactions = new java.util.HashMap<>();

    public SocialMediaPost() {}

    public SocialMediaPost(String id, String content, String author, LocalDateTime timestamp, String platform) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.timestamp = timestamp;
        this.platform = platform;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getShareCount() {
        return shareCount;
    }

    public void setShareCount(int shareCount) {
        this.shareCount = shareCount;
    }

    public java.util.List<String> getComments() {
        if (comments == null) {
            comments = new java.util.ArrayList<>();
        }
        return comments;
    }

    public void setComments(java.util.List<String> comments) {
        this.comments = comments;
    }

    public java.util.Map<String, Integer> getReactions() {
        if (reactions == null) {
            reactions = new java.util.HashMap<>();
        }
        return reactions;
    }

    public void setReactions(java.util.Map<String, Integer> reactions) {
        this.reactions = reactions;
    }

    public String getReactionsString() {
        if (reactions == null || reactions.isEmpty()) return "None";
        java.util.List<String> list = new java.util.ArrayList<>();
        if (reactions.containsKey("like") && reactions.get("like") > 0) list.add("👍 Like: " + reactions.get("like"));
        if (reactions.containsKey("love") && reactions.get("love") > 0) list.add("❤️ Love: " + reactions.get("love"));
        if (reactions.containsKey("care") && reactions.get("care") > 0) list.add("🥰 Care: " + reactions.get("care"));
        if (reactions.containsKey("sad") && reactions.get("sad") > 0) list.add("😢 Sad: " + reactions.get("sad"));
        if (reactions.containsKey("angry") && reactions.get("angry") > 0) list.add("😡 Angry: " + reactions.get("angry"));
        if (reactions.containsKey("haha") && reactions.get("haha") > 0) list.add("😆 Haha: " + reactions.get("haha"));
        if (reactions.containsKey("wow") && reactions.get("wow") > 0) list.add("😮 Wow: " + reactions.get("wow"));
        
        for (java.util.Map.Entry<String, Integer> entry : reactions.entrySet()) {
            String key = entry.getKey();
            if (!java.util.Arrays.asList("like", "love", "care", "sad", "angry", "haha", "wow").contains(key.toLowerCase()) && entry.getValue() > 0) {
                list.add(key.substring(0, 1).toUpperCase() + key.substring(1) + ": " + entry.getValue());
            }
        }
        
        if (list.isEmpty()) return "None";
        return String.join(" | ", list);
    }

    public String getCommentsString() {
        if (comments == null || comments.isEmpty()) return "None";
        java.util.List<String> list = new java.util.ArrayList<>();
        for (int i = 0; i < comments.size(); i++) {
            list.add("💬 " + (i + 1) + ". \"" + comments.get(i) + "\"");
        }
        return String.join("\n", list);
    }

    @Override
    public String toString() {
        return "SocialMediaPost{" +
                "id='" + id + '\'' +
                ", platform='" + platform + '\'' +
                ", author='" + author + '\'' +
                ", content='" + content + '\'' +
                ", likes=" + likeCount +
                ", shares=" + shareCount +
                ", commentsCount=" + (comments != null ? comments.size() : 0) +
                '}';
    }
}

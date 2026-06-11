package main.java.com.project.datacollection.model;

import java.time.LocalDateTime;

public class SocialMediaPost {
    private String id;
    private String content;
    private String author;
    private LocalDateTime timestamp;
    private String platform;
    private int likeCount;
    private int shareCount;

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

    @Override
    public String toString() {
        return "SocialMediaPost{" +
                "id='" + id + '\'' +
                ", platform='" + platform + '\'' +
                ", author='" + author + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}

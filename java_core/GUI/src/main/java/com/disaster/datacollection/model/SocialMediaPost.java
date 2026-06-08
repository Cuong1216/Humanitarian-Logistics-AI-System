package com.disaster.datacollection.model;

import java.util.Date;

public class SocialMediaPost {
    private String id;
    private String content;
    private Date timestamp;

    public SocialMediaPost() {}

    public SocialMediaPost(String id, String content, Date timestamp) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "[" + id + "] " + content;
    }
}

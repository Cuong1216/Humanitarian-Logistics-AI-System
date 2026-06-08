package com.disaster.ai_client;

import java.util.List;

public class AIRequestDTO {
    private String model;
    private List<String> posts;
    private String task;

    public AIRequestDTO() {}
    public AIRequestDTO(String model, List<String> posts, String task) {
        this.model = model;
        this.posts = posts;
        this.task = task;
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<String> getPosts() { return posts; }
    public void setPosts(List<String> posts) { this.posts = posts; }
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
}

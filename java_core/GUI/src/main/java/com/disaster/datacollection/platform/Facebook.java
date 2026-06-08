package com.disaster.datacollection.platform;

import com.disaster.datacollection.model.SocialMediaPost;
import java.util.*;

public class Facebook implements Platform {
    @Override
    public List<SocialMediaPost> fetchPost(String keyword, Date startDate, Date endDate) {
        // Mock data for demonstration
        List<SocialMediaPost> posts = new ArrayList<>();
        posts.add(new SocialMediaPost("fb_001", "[Facebook] Flood in district 7: " + keyword, new Date()));
        posts.add(new SocialMediaPost("fb_002", "[Facebook] Need medical supplies urgently: " + keyword, new Date()));
        return posts;
    }
}

package com.disaster.datacollection.platform;

import com.disaster.datacollection.model.SocialMediaPost;
import java.util.*;

public class Twitter implements Platform {
    @Override
    public List<SocialMediaPost> fetchPost(String keyword, Date startDate, Date endDate) {
        List<SocialMediaPost> posts = new ArrayList<>();
        posts.add(new SocialMediaPost("tw_001", "[Twitter] #disaster road blocked near bridge: " + keyword, new Date()));
        posts.add(new SocialMediaPost("tw_002", "[Twitter] #relief families stranded need rescue: " + keyword, new Date()));
        return posts;
    }
}

package com.project.datacollection.platform;

import com.project.datacollection.model.SocialMediaPost;
import java.util.List;

public interface Platform {
    List<SocialMediaPost> scrapePosts(String keyword);
    String getPlatformName();
}

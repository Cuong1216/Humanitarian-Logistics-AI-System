package com.disaster.datacollection.platform;

import com.disaster.datacollection.model.SocialMediaPost;
import java.util.Date;
import java.util.List;

public interface Platform {
    List<SocialMediaPost> fetchPost(String keyword, Date startDate, Date endDate);
}

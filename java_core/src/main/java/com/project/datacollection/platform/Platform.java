<<<<<<< HEAD
package main.java.com.project.datacollection.platform;

import main.java.com.project.datacollection.model.SocialMediaPost;

import java.util.Date;
import java.util.List;

public interface Platform {
    List<SocialMediaPost> fetchPost(String keyword, Date startDate, Date endDate);
    String getPlatformName();
}

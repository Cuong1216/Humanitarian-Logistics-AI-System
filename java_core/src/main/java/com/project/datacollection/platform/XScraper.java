package com.project.datacollection.platform;

import com.project.datacollection.model.SocialMediaPost;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XScraper implements Platform {

    @Override
    public List<SocialMediaPost> fetchPost(String keyword, Date startDate, Date endDate) {
        List<SocialMediaPost> posts = crawlLiveX(keyword);
        
        if (posts == null || posts.isEmpty()) {
            throw new RuntimeException("[ERROR] Twitter/X rate limiting or login wall blocked the request. Automated crawl failed to retrieve real-time data from X.");
        } else {
            System.out.println("[*] Successfully crawled " + posts.size() + " live posts from X.");
        }
        
        return posts;
    }

    @Override
    public String getPlatformName() {
        return "X";
    }

    private List<SocialMediaPost> crawlLiveX(String keyword) {
        List<SocialMediaPost> scrapedPosts = new ArrayList<>();
        System.out.println("[*] Attemping to query live public tweets for: " + keyword + " ...");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            String searchUrl = "https://x.com/search?q=" + keyword.replace(" ", "%20") + "&src=typed_query";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String html = response.body();
                Pattern pattern = Pattern.compile("(?s)TweetTextSize.*?>(.*?)<");
                Matcher matcher = pattern.matcher(html);
                int count = 0;
                while (matcher.find() && count < 3) {
                    String text = matcher.group(1).replaceAll("<[^>]+>", "").trim();
                    if (text.length() > 20) {
                        scrapedPosts.add(new SocialMediaPost(
                            "scraped-x-" + count + "-" + UUID.randomUUID().toString().substring(0, 8),
                            text,
                            "User_Live",
                            LocalDateTime.now(),
                            "X"
                        ));
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[!] Error querying live X: " + e.getMessage());
        }
        return scrapedPosts.isEmpty() ? null : scrapedPosts;
    }
}

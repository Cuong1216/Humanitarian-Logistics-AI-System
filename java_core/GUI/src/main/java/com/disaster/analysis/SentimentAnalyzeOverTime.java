package com.disaster.analysis;

import com.disaster.ai_client.AiClient;
import com.disaster.datacollection.model.SocialMediaPost;
import java.util.*;

public class SentimentAnalyzeOverTime implements TaskAnalyzer {

    @Override
    public AnalysisResult analyze(List<SocialMediaPost> posts, AiClient aiClient) {
        AnalysisResult result = new AnalysisResult("SentimentOverTime");
        // Mock: assign random sentiment scores per post
        Map<String, Double> sentimentMap = new LinkedHashMap<>();
        String[] sentiments = {"Panic", "Fear", "Neutral", "Hope", "Relief"};
        Random rand = new Random();
        for (SocialMediaPost post : posts) {
            sentimentMap.put(post.getId(), rand.nextDouble() * 2 - 1); // -1.0 to 1.0
        }
        result.put("sentimentTimeline", sentimentMap);
        result.setSummary("Sentiment analyzed over " + posts.size() + " posts.");
        return result;
    }
}

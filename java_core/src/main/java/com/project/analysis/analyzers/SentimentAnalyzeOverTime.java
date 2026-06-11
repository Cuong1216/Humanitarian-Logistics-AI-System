package com.project.analysis.analyzers;

import com.project.analysis.AnalysisResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SentimentAnalyzeOverTime {

    /**
     * Aggregate the sentiment trends from a list of processing results.
     * Useful for building time-series charts on social media sentiment.
     */
    public Map<String, Long> aggregateSentimentTrends(List<AnalysisResult> results) {
        return results.stream()
                .filter(r -> r.getMetrics().containsKey("Sentiment"))
                .collect(Collectors.groupingBy(
                        r -> (String) r.getMetrics().get("Sentiment"),
                        Collectors.counting()
                ));
    }
}

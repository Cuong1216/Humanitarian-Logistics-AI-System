package com.project.analysis;

import java.util.HashMap;
import java.util.Map;

public class AnalysisResult {
    private String originalText;
    private Map<String, Object> metrics;

    public AnalysisResult(String originalText) {
        this.originalText = originalText;
        this.metrics = new HashMap<>();
    }

    public String getOriginalText() {
        return originalText;
    }

    public void addMetric(String key, Object value) {
        metrics.put(key, value);
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }
}

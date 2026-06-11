package com.disaster.analysis;

import java.util.HashMap;
import java.util.Map;

public class AnalysisResult {
    private Map<String, Object> data = new HashMap<>();
    private String analyzerName;
    private String summary;

    public AnalysisResult() {}
    public AnalysisResult(String analyzerName) {
        this.analyzerName = analyzerName;
    }

    public void put(String key, Object value) { data.put(key, value); }
    public Object get(String key) { return data.get(key); }
    public Map<String, Object> getData() { return data; }

    public String getAnalyzerName() { return analyzerName; }
    public void setAnalyzerName(String analyzerName) { this.analyzerName = analyzerName; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    @Override
    public String toString() {
        return "[" + analyzerName + "] " + (summary != null ? summary : data.toString());
    }
}

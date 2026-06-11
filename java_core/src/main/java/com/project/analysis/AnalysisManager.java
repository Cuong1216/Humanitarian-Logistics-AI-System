package com.project.analysis;

import java.util.ArrayList;
import java.util.List;

public class AnalysisManager {
    private List<TaskAnalyzer> analyzers;

    public AnalysisManager() {
        this.analyzers = new ArrayList<>();
    }

    public void addAnalyzer(TaskAnalyzer analyzer) {
        this.analyzers.add(analyzer);
    }

    public void removeAnalyzer(TaskAnalyzer analyzer) {
        this.analyzers.remove(analyzer);
    }

    public void removeAllAnalyzers() {
        this.analyzers.clear();
    }

    public AnalysisResult processText(String text) {
        AnalysisResult result = new AnalysisResult(text);
        for (TaskAnalyzer analyzer : analyzers) {
            analyzer.analyze(text, result);
        }
        return result;
    }

    public List<AnalysisResult> processTexts(List<String> texts) {
        List<AnalysisResult> results = new ArrayList<>();
        for (String text : texts) {
            results.add(processText(text));
        }
        return results;
    }

    public List<AnalysisResult> runAll(List<String> texts) {
        return processTexts(texts);
    }
}

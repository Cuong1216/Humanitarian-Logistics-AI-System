package com.project.analysis.analyzers;

import com.project.analysis.AnalysisResult;
import com.project.analysis.TaskAnalyzer;

public class DamageCategorizer implements TaskAnalyzer {
    @Override
    public void analyze(String text, AnalysisResult result) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("destroy") || lowerText.contains("collapse") || lowerText.contains("dead")
                || lowerText.contains("phá hủy") || lowerText.contains("sụp đổ") || lowerText.contains("chết") || lowerText.contains("thiệt mạng")) {
            result.addMetric("DamageLevel", "SEVERE");
        } else if (lowerText.contains("damage") || lowerText.contains("flood") || lowerText.contains("hurt")
                || lowerText.contains("thiệt hại") || lowerText.contains("lũ lụt") || lowerText.contains("ngập") || lowerText.contains("bị thương")) {
            result.addMetric("DamageLevel", "MODERATE");
        } else {
            result.addMetric("DamageLevel", "LOW");
        }
    }
}

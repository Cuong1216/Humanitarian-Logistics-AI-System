package com.project.analysis.analyzers;

import com.project.analysis.AnalysisResult;
import com.project.analysis.TaskAnalyzer;
import java.util.ArrayList;
import java.util.List;

public class ReliefSentimentAnalyzer implements TaskAnalyzer {
    @Override
    public void analyze(String text, AnalysisResult result) {
        String lowerText = text.toLowerCase();
        
        List<String> items = new ArrayList<>();
        
        if (lowerText.contains("food") || lowerText.contains("lương thực") || lowerText.contains("thức ăn") || lowerText.contains("gạo") || lowerText.contains("mì tôm")) {
            items.add("Food");
        }
        if (lowerText.contains("water") || lowerText.contains("nước uống") || lowerText.contains("nước sạch")) {
            items.add("Water");
        }
        if (lowerText.contains("medicine") || lowerText.contains("thuốc") || lowerText.contains("y tế")) {
            items.add("Medicine");
        }
        if (lowerText.contains("clothes") || lowerText.contains("quần áo") || lowerText.contains("chăn ấm")) {
            items.add("Clothes");
        }

        boolean hasItems = !items.isEmpty();

        if (hasItems || lowerText.contains("help") || lowerText.contains("rescue") || lowerText.contains("urgent")
                || lowerText.contains("cứu") || lowerText.contains("cứu trợ") || lowerText.contains("cứu hộ")
                || lowerText.contains("khẩn cấp")) {
            result.addMetric("ReliefNeeded", true);
            result.addMetric("Sentiment", "DESPERATE");
            if (hasItems) {
                result.addMetric("ReliefItems", String.join(", ", items));
            }
        } else {
            result.addMetric("ReliefNeeded", false);
            result.addMetric("Sentiment", "NEUTRAL");
        }
    }
}

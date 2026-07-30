package com.project.analysis;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WordPreprocessor {

    // Regex patterns matching the Python TextCleaningService
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+|www\\.\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\w+");
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");
    private static final Pattern EXTRA_SYMBOL_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s\\p{L}.,!?;:%/-]");
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    // Common Vietnamese stopwords
    private static final Set<String> VIETNAMESE_STOPWORDS = new HashSet<>(Arrays.asList(
        "và", "là", "thì", "mà", "của", "được", "bị", "có", "trong", "cho",
        "ở", "này", "cơ", "những", "các", "cái", "ra", "với", "tại", "vào",
        "sẽ", "đã", "đang", "từ", "đến", "cũng", "để", "như", "nhưng", "nếu",
        "vì", "nên", "cho", "hộ", "nhờ", "qua", "lại", "nhiều", "ít", "đang"
    ));

    /**
     * Cleans text based on standard NLP pre-processing rules (matching TextCleaningService.py).
     */
    public static String cleanText(String text) {
        if (text == null) return "";
        
        // Normalize Unicode to NFC
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        
        // Remove URLs
        normalized = URL_PATTERN.matcher(normalized).replaceAll(" ");
        
        // Remove @mentions
        normalized = MENTION_PATTERN.matcher(normalized).replaceAll(" ");
        
        // Unwrap hashtags (#BaoSo3 -> BaoSo3)
        normalized = HASHTAG_PATTERN.matcher(normalized).replaceAll("$1");
        
        // Remove extra symbols/special chars
        normalized = EXTRA_SYMBOL_PATTERN.matcher(normalized).replaceAll(" ");
        
        // Normalize spaces
        normalized = SPACE_PATTERN.matcher(normalized).replaceAll(" ");
        
        return normalized.trim();
    }

    /**
     * Removes common Vietnamese stopwords from the cleaned text.
     */
    public static String removeStopwords(String cleanedText) {
        if (cleanedText == null || cleanedText.isEmpty()) return "";
        
        String lower = cleanedText.toLowerCase();
        
        // Split by whitespace
        String[] tokens = lower.split("\\s+");
        
        // Filter out stopwords
        return Arrays.stream(tokens)
            .filter(token -> !VIETNAMESE_STOPWORDS.contains(token))
            .collect(Collectors.joining(" "));
    }

    /**
     * Clean and filter stopwords in a single preprocess pipeline.
     */
    public static String preprocess(String text) {
        String cleaned = cleanText(text);
        return removeStopwords(cleaned);
    }
}

import re

from schemas import EmotionLabel, EmotionScore

VIETNAMESE_EMOTION_HINTS = {
    EmotionLabel.ANGER: {"bat binh", "bất bình", "phan no", "phẫn nộ", "tuc gian", "tức giận"},
    EmotionLabel.FEAR: {"hoang loan", "hoảng loạn", "lo lang", "lo lắng", "nguy hiem", "nguy hiểm", "sợ"},
    EmotionLabel.SADNESS: {"buon", "buồn", "chet", "chết", "mất", "thuong tam", "thương tâm"},
    EmotionLabel.DISGUST: {"bẩn", "o nhiem", "ô nhiễm", "kinh khung", "kinh khủng"},
    EmotionLabel.JOY: {"cảm ơn", "cam on", "vui mừng", "vui mung", "phấn khởi", "phan khoi", "biết ơn", "biet on", "tốt đẹp", "tot dep"},
}

class EmotionDetector:
    """
    Xử lý nhận diện cảm xúc dựa trên từ khóa tiếng Việt và mock điểm cảm xúc.
    """

    def dominant_emotion(self, text: str, negative_score: float) -> EmotionLabel:
        for emotion, hints in VIETNAMESE_EMOTION_HINTS.items():
            for hint in hints:
                pattern = rf"(?<!\w){re.escape(hint)}(?!\w)"
                if re.search(pattern, text):
                    return emotion
        if negative_score >= 0.35:
            return EmotionLabel.FEAR
        return EmotionLabel.NEUTRAL

    def mock_emotion_scores(self, dominant: EmotionLabel, negative_score: float) -> list[EmotionScore]:
        scores = {label: 0.04 for label in EmotionLabel}
        scores[dominant] = max(0.4, negative_score)
        scores[EmotionLabel.NEUTRAL] = max(0.05, 1 - negative_score)
        if dominant != EmotionLabel.FEAR:
            scores[EmotionLabel.FEAR] = max(scores[EmotionLabel.FEAR], negative_score * 0.45)
        return [EmotionScore(label=label, score=round(min(1.0, value), 3)) for label, value in scores.items()]

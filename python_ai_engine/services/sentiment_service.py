from schemas import AnalysisResult, EmotionLabel


class SentimentService:
    NEGATIVE_EMOTIONS = {
        EmotionLabel.ANGER,
        EmotionLabel.FEAR,
        EmotionLabel.SADNESS,
        EmotionLabel.DISGUST,
    }

    def is_negative(self, result: AnalysisResult, threshold: float = 0.35) -> bool:
        return result.negative_score >= threshold or result.dominant_emotion in self.NEGATIVE_EMOTIONS

    def risk_score(self, result: AnalysisResult) -> float:
        urgency_weight = {
            "low": 0.1,
            "medium": 0.45,
            "high": 0.75,
            "critical": 1.0,
        }[result.humanitarian_signal.urgency.value]
        return round((result.negative_score * 0.65) + (urgency_weight * 0.35), 3)

import json
import re
from typing import Any
from schemas import (
    AnalysisResult,
    EmotionLabel,
    EmotionScore,
    HumanitarianSignal,
    NeedCategory,
    SocialMediaPost,
    UrgencyLevel,
)
from services.ner_service import NerService

class ResponseParser:
    """
    Parse JSON từ response của LLM và map vào dataclass/pydantic models.
    """

    def __init__(self, ner_service: NerService) -> None:
        self.ner = ner_service

    def extract_json(self, text: str) -> dict[str, Any]:
        cleaned = text.strip()
        cleaned = re.sub(r"^```(?:json)?", "", cleaned).strip()
        cleaned = re.sub(r"```$", "", cleaned).strip()
        try:
            return json.loads(cleaned)
        except json.JSONDecodeError:
            match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
            if not match:
                raise
            return json.loads(match.group(0))

    def result_from_payload(
        self,
        post: SocialMediaPost,
        payload: dict[str, Any],
        source: str,
        combined_text: str,
        raw: dict[str, Any] | None = None,
    ) -> AnalysisResult:
        scores_payload = payload.get("emotion_scores", {})
        scores = [
            EmotionScore(label=label, score=float(scores_payload.get(label.value, 0)))
            for label in EmotionLabel
        ]
        categories = [
            self._enum_or_default(NeedCategory, value, NeedCategory.UNKNOWN)
            for value in payload.get("categories", [])
        ]
        if not categories:
            categories = self.ner.extract_needs(combined_text) or [NeedCategory.UNKNOWN]

        locations = [str(item) for item in payload.get("locations", [])]
        if not locations:
            locations = self.ner.extract_locations(post.text, post.location_hint)

        return AnalysisResult(
            post_id=post.id,
            keyword=post.keyword,
            dominant_emotion=self._enum_or_default(
                EmotionLabel,
                payload.get("dominant_emotion"),
                EmotionLabel.MIXED,
            ),
            emotion_scores=scores,
            negative_score=self._clamp(payload.get("negative_score", 0.0)),
            confidence=self._clamp(payload.get("confidence", 0.7)),
            humanitarian_signal=HumanitarianSignal(
                is_emergency=bool(payload.get("is_emergency", False)),
                urgency=self._enum_or_default(
                    UrgencyLevel,
                    payload.get("urgency"),
                    UrgencyLevel.LOW,
                ),
                categories=categories,
                locations=locations,
                affected_people_estimate=payload.get("affected_people_estimate"),
                recommended_action=str(payload.get("recommended_action", "Tiếp tục theo dõi tình hình.")),
            ),
            summary=str(payload.get("summary", "Chưa có tóm tắt.")),
            source=source,
            raw=raw or {},
        )

    def _clamp(self, value: Any) -> float:
        try:
            return max(0.0, min(1.0, float(value)))
        except (TypeError, ValueError):
            return 0.0

    def _enum_or_default(self, enum_cls: type, value: Any, default: Any) -> Any:
        try:
            return enum_cls(value)
        except ValueError:
            return default

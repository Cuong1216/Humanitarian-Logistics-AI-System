import json
import os
import re
from collections import Counter
from typing import Any

try:
    from dotenv import load_dotenv
except ImportError:
    def load_dotenv() -> bool:
        return False

from schemas import (
    AnalysisResult,
    EmotionLabel,
    EmotionScore,
    HumanitarianSignal,
    NeedCategory,
    SocialMediaPost,
    UrgencyLevel,
)
from services.knn_severity_service import KnnSeverityService
from services.ner_service import NerService
from services.text_cleaning_service import TextCleaningService

load_dotenv()


TRIGGER_WORDS = {
    "bao",
    "bão",
    "bao so",
    "bão số",
    "bi cuon troi",
    "bị cuốn trôi",
    "bi thuong",
    "bị thương",
    "can cuu",
    "cần cứu",
    "can giup",
    "cần giúp",
    "chet",
    "chết",
    "cuu",
    "cứu",
    "cuu ho",
    "cứu hộ",
    "cuu nan",
    "cứu nạn",
    "cuu voi",
    "cứu với",
    "doi",
    "đói",
    "duong bi cat",
    "đường bị cắt",
    "khan",
    "khẩn",
    "khan cap",
    "khẩn cấp",
    "khat",
    "khát",
    "lu",
    "lũ",
    "lu lut",
    "lũ lụt",
    "mac ket",
    "mắc kẹt",
    "mat tich",
    "mất tích",
    "mat nha",
    "mất nhà",
    "ngap",
    "ngập",
    "nguy cap",
    "nguy cấp",
    "nguy hiem",
    "nguy hiểm",
    "nuoc cuon",
    "nước cuốn",
    "sap",
    "sập",
    "sat lo",
    "sạt lở",
    "so tan",
    "sơ tán",
    "thieu",
    "thiếu",
    "thieu do an",
    "thiếu đồ ăn",
    "thieu nuoc",
    "thiếu nước",
    "thieu thuoc",
    "thiếu thuốc",
    "thuong vong",
    "thương vong",
    "y te",
    "y tế",
}

VIETNAMESE_EMOTION_HINTS = {
    EmotionLabel.ANGER: {"bat binh", "bất bình", "phan no", "phẫn nộ", "tuc gian", "tức giận"},
    EmotionLabel.FEAR: {"hoang loan", "hoảng loạn", "lo lang", "lo lắng", "nguy hiem", "nguy hiểm", "so", "sợ"},
    EmotionLabel.SADNESS: {"buon", "buồn", "chet", "chết", "mat", "mất", "thuong tam", "thương tâm"},
    EmotionLabel.DISGUST: {"ban", "bẩn", "o nhiem", "ô nhiễm", "kinh khung", "kinh khủng"},
}


class NlpService:
    def __init__(self) -> None:
        self.api_key = os.getenv("GEMINI_API_KEY", "").strip()
        self.model = os.getenv("GEMINI_MODEL", "gemini-2.0-flash").strip()
        self.use_mock = os.getenv("USE_MOCK_AI", "false").lower() == "true"
        self.cleaner = TextCleaningService()
        self.ner = NerService()
        self.knn = KnnSeverityService(k=3)
        self._client = None

        if self.api_key and not self.use_mock:
            try:
                from google import genai

                self._client = genai.Client(api_key=self.api_key)
            except Exception:
                self._client = None

    @property
    def gemini_enabled(self) -> bool:
        return self._client is not None and not self.use_mock

    def analyze_post(self, post: SocialMediaPost) -> AnalysisResult:
        cleaned_post = self._clean_post(post)
        if self.gemini_enabled:
            try:
                return self._analyze_with_gemini(cleaned_post)
            except Exception as exc:
                result = self._analyze_with_mock(cleaned_post)
                result.raw["gemini_error"] = str(exc)
                return result

        return self._analyze_with_mock(cleaned_post)

    def _clean_post(self, post: SocialMediaPost) -> SocialMediaPost:
        return post.model_copy(
            update={
                "text": self.cleaner.clean(post.text),
                "comments": self.cleaner.clean_many(post.comments),
            }
        )

    def _analyze_with_gemini(self, post: SocialMediaPost) -> AnalysisResult:
        prompt = self._build_prompt(post)
        response = self._client.models.generate_content(model=self.model, contents=prompt)
        text = getattr(response, "text", "") or ""
        payload = self._extract_json(text)
        result = self._result_from_payload(post, payload, source="gemini", raw={"gemini_text": text})
        return self._apply_rule_based_urgency(post, result)

    def _build_prompt(self, post: SocialMediaPost) -> str:
        comments = "\n".join(f"- {comment}" for comment in post.comments[:20])
        reactions = json.dumps(post.reactions, ensure_ascii=False)
        return f"""
Bạn là chuyên gia trích xuất thông tin cứu trợ cho bài toán cứu trợ nhân đạo sau thiên tai tại Việt Nam.
Hãy đọc nội dung mạng xã hội đã được làm sạch và trả về CHỈ JSON hợp lệ, không markdown.

Schema JSON bắt buộc:
{{
  "dominant_emotion": "anger|fear|sadness|disgust|joy|neutral|mixed",
  "emotion_scores": {{"anger": 0.0, "fear": 0.0, "sadness": 0.0, "disgust": 0.0, "joy": 0.0, "neutral": 0.0, "mixed": 0.0}},
  "negative_score": 0.0,
  "confidence": 0.0,
  "is_emergency": true,
  "urgency": "low|medium|high|critical",
  "categories": ["food", "water", "medical", "shelter", "rescue", "transport", "sanitation", "unknown"],
  "locations": ["location names"],
  "affected_people_estimate": null,
  "recommended_action": "short logistics action in Vietnamese",
  "summary": "one short sentence in Vietnamese"
}}

Quy tắc:
- Nếu nội dung tiêu cực và có từ khóa cứu trợ/thiên tai như "cứu với", "sập", "lũ", "ngập", "chết", "đói", "y tế", "khẩn cấp", hãy đánh dấu khẩn cấp.
- Trích xuất địa điểm tiếng Việt vào "locations", ví dụ: "thôn A", "xã Bình Minh", "huyện Sơn Động".
- Nếu văn bản có 'location_hint', hãy ưu tiên lấy thông tin đó.
- Trích xuất nhu cầu cứu trợ vào "categories". Giá trị category vẫn phải dùng enum tiếng Anh trong schema.
- "recommended_action" và "summary" viết bằng tiếng Việt ngắn gọn.

Từ khóa theo dõi: {post.keyword}
Gợi ý địa điểm: {post.location_hint}
Nền tảng: {post.platform}
Nội dung đã làm sạch: {post.text}
Lượt phản ứng: {reactions}
Bình luận đã làm sạch:
{comments}
"""

    def _extract_json(self, text: str) -> dict[str, Any]:
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

    def _result_from_payload(
        self,
        post: SocialMediaPost,
        payload: dict[str, Any],
        source: str,
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
            categories = self.ner.extract_needs(self._combined_text(post)) or [NeedCategory.UNKNOWN]

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

    def _analyze_with_mock(self, post: SocialMediaPost) -> AnalysisResult:
        combined_text = self._combined_text(post)
        trigger_hits = self._count_trigger_hits(combined_text)
        need_categories = self.ner.extract_needs(combined_text)
        locations = self.ner.extract_locations(post.text, post.location_hint)
        reaction_pressure = self._reaction_pressure(post.reactions)

        base_negative = min(
            1.0,
            (trigger_hits / 8) + reaction_pressure + (0.15 if need_categories else 0),
        )
        is_emergency = base_negative >= 0.35 and trigger_hits > 0
        rule_urgency = self._urgency_from_score(base_negative, need_categories, combined_text, trigger_hits)
        knn_result = self.knn.predict(post, trigger_hits, need_categories, base_negative)
        knn_urgency = self._enum_or_default(UrgencyLevel, knn_result["predicted_urgency"], UrgencyLevel.LOW)
        urgency = self._max_urgency(rule_urgency, knn_urgency)
        dominant = self._dominant_emotion(combined_text, base_negative)
        emotion_scores = self._mock_emotion_scores(dominant, base_negative)

        return AnalysisResult(
            post_id=post.id,
            keyword=post.keyword,
            dominant_emotion=dominant,
            emotion_scores=emotion_scores,
            negative_score=round(base_negative, 3),
            confidence=0.76 if is_emergency else 0.64,
            humanitarian_signal=HumanitarianSignal(
                is_emergency=is_emergency,
                urgency=urgency,
                categories=need_categories or [NeedCategory.UNKNOWN],
                locations=locations,
                affected_people_estimate=self._estimate_people(combined_text),
                recommended_action=self._recommended_action(urgency, need_categories, locations),
            ),
            summary=self._summary(post, urgency, need_categories, locations),
            source="mock",
            raw={
                "cleaned_text": post.text,
                "trigger_hits": trigger_hits,
                "reaction_pressure": reaction_pressure,
                "knn_severity": knn_result,
                "extraction_mode": "regex_fallback",
            },
        )

    def _apply_rule_based_urgency(self, post: SocialMediaPost, result: AnalysisResult) -> AnalysisResult:
        combined_text = self._combined_text(post)
        trigger_hits = self._count_trigger_hits(combined_text)
        needs = self.ner.extract_needs(combined_text)
        locations = self.ner.extract_locations(post.text, post.location_hint)

        categories = list(dict.fromkeys([*result.humanitarian_signal.categories, *needs]))
        if NeedCategory.UNKNOWN in categories and len(categories) > 1:
            categories.remove(NeedCategory.UNKNOWN)

        final_urgency = result.humanitarian_signal.urgency
        knn_result = self.knn.predict(post, trigger_hits, categories, result.negative_score)
        knn_urgency = self._enum_or_default(UrgencyLevel, knn_result["predicted_urgency"], UrgencyLevel.LOW)
        final_urgency = self._max_urgency(final_urgency, knn_urgency)
        is_emergency = result.humanitarian_signal.is_emergency
        if result.negative_score >= 0.5 and trigger_hits > 0:
            is_emergency = True
            final_urgency = self._urgency_from_score(
                result.negative_score,
                categories,
                combined_text,
                trigger_hits,
            )

        result.humanitarian_signal.is_emergency = is_emergency
        result.humanitarian_signal.urgency = final_urgency
        result.humanitarian_signal.categories = categories or [NeedCategory.UNKNOWN]
        result.humanitarian_signal.locations = list(
            dict.fromkeys([*result.humanitarian_signal.locations, *locations])
        )
        result.humanitarian_signal.recommended_action = self._recommended_action(
            final_urgency,
            result.humanitarian_signal.categories,
            result.humanitarian_signal.locations,
        )
        result.raw["cleaned_text"] = post.text
        result.raw["trigger_hits"] = trigger_hits
        result.raw["knn_severity"] = knn_result
        result.raw["extraction_mode"] = "gemini_plus_regex_fallback"
        return result

    def _combined_text(self, post: SocialMediaPost) -> str:
        return " ".join([post.text, *post.comments]).lower()

    def _count_trigger_hits(self, text: str) -> int:
        return sum(1 for trigger in TRIGGER_WORDS if trigger in text)

    def _reaction_pressure(self, reactions: dict[str, int]) -> float:
        sad = reactions.get("sad", 0) + reactions.get("care", 0)
        angry = reactions.get("angry", 0)
        total = sum(max(0, value) for value in reactions.values())
        if total == 0:
            return 0.0
        return min(0.25, ((sad + angry) / total) * 0.25)

    def _urgency_from_score(
        self,
        score: float,
        categories: list[NeedCategory],
        text: str,
        trigger_hits: int,
    ) -> UrgencyLevel:
        if (
            "mac ket" in text
            or "mắc kẹt" in text
            or "cuu voi" in text
            or "cứu với" in text
            or NeedCategory.RESCUE in categories
            or score >= 0.8
            or trigger_hits >= 5
        ):
            return UrgencyLevel.CRITICAL
        if score >= 0.6 or NeedCategory.MEDICAL in categories or trigger_hits >= 3:
            return UrgencyLevel.HIGH
        if score >= 0.35 or categories:
            return UrgencyLevel.MEDIUM
        return UrgencyLevel.LOW

    def _max_urgency(self, first: UrgencyLevel, second: UrgencyLevel) -> UrgencyLevel:
        order = {
            UrgencyLevel.LOW: 0,
            UrgencyLevel.MEDIUM: 1,
            UrgencyLevel.HIGH: 2,
            UrgencyLevel.CRITICAL: 3,
        }
        return first if order[first] >= order[second] else second
    def _dominant_emotion(self, text: str, negative_score: float) -> EmotionLabel:
        for emotion, hints in VIETNAMESE_EMOTION_HINTS.items():
            if any(token in text for token in hints):
                return emotion
        if negative_score >= 0.35:
            return EmotionLabel.FEAR
        return EmotionLabel.NEUTRAL

    def _mock_emotion_scores(self, dominant: EmotionLabel, negative_score: float) -> list[EmotionScore]:
        scores = {label: 0.04 for label in EmotionLabel}
        scores[dominant] = max(0.4, negative_score)
        scores[EmotionLabel.NEUTRAL] = max(0.05, 1 - negative_score)
        if dominant != EmotionLabel.FEAR:
            scores[EmotionLabel.FEAR] = max(scores[EmotionLabel.FEAR], negative_score * 0.45)
        return [EmotionScore(label=label, score=round(min(1.0, value), 3)) for label, value in scores.items()]

    def _recommended_action(
        self,
        urgency: UrgencyLevel,
        categories: list[NeedCategory],
        locations: list[str],
    ) -> str:
        target = locations[0] if locations else "khu vực được nhắc đến"
        needs = ", ".join(category.to_vietnamese() for category in categories) if categories else "xác minh thực địa"
        if urgency in {UrgencyLevel.CRITICAL, UrgencyLevel.HIGH}:
            return f"Điều phối đội cứu trợ và hàng hóa gồm {needs} đến {target} ngay lập tức."
        if urgency == UrgencyLevel.MEDIUM:
            return f"Ưu tiên xác minh thông tin và chuẩn bị hỗ trợ {needs} cho {target}."
        return f"Theo dõi thêm tín hiệu mạng xã hội tại {target}."

    def _summary(
        self,
        post: SocialMediaPost,
        urgency: UrgencyLevel,
        categories: list[NeedCategory],
        locations: list[str],
    ) -> str:
        target = locations[0] if locations else post.keyword or "khu vực được nhắc đến"
        needs = ", ".join(category.to_vietnamese() for category in categories) if categories else "nhu cầu chưa rõ"
        urgency_vi = {
            UrgencyLevel.LOW: "thấp",
            UrgencyLevel.MEDIUM: "trung bình",
            UrgencyLevel.HIGH: "cao",
            UrgencyLevel.CRITICAL: "rất khẩn cấp",
        }[urgency]
        return f"{target} có mức độ khẩn cấp {urgency_vi}, ghi nhận nhu cầu: {needs}."



    def _estimate_people(self, text: str) -> int | None:
        numbers = [int(item) for item in re.findall(r"\b\d{1,6}\b", text)]
        return max(numbers) if numbers else None

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


def top_locations(results: list[AnalysisResult]) -> list[str]:
    counter: Counter[str] = Counter()
    for result in results:
        counter.update(result.humanitarian_signal.locations)
    return [location for location, _ in counter.most_common(5)]



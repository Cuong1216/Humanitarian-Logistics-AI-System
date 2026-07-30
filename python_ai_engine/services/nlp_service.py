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
    HumanitarianSignal,
    NeedCategory,
    SocialMediaPost,
    UrgencyLevel,
)
from services.knn_severity_service import KnnSeverityService
from services.ner_service import NerService
from services.text_cleaning_service import TextCleaningService
from services.prompt_builder import PromptBuilder
from services.response_parser import ResponseParser
from services.urgency_scorer import UrgencyScorer
from services.emotion_detector import EmotionDetector
from services.action_generator import ActionGenerator
from services.cache_service import CacheService
from services.text_utils import estimate_people_count
import logging

logger = logging.getLogger("ai_engine")

load_dotenv()

TRIGGER_WORDS = {
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
    "cứu",
    "cuu ho",
    "cứu hộ",
    "cuu nan",
    "cứu nạn",
    "cuu voi",
    "cứu với",
    "đói",
    "duong bi cat",
    "đường bị cắt",
    "khẩn",
    "khan cap",
    "khẩn cấp",
    "khat",
    "khát",
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
    "sập",
    "sat lo",
    "sạt lở",
    "so tan",
    "sơ tán",
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

# Pre-compiled tại module load time — chỉ compile 1 lần
_TRIGGER_PATTERN = re.compile(
    r"(?<!\w)(" + "|".join(re.escape(t) for t in sorted(TRIGGER_WORDS, key=len, reverse=True)) + r")(?!\w)",
    flags=re.IGNORECASE,
)

class NlpService:
    def __init__(
        self,
        cleaner: TextCleaningService = None,
        ner: NerService = None,
        knn: KnnSeverityService = None,
        prompt_builder: PromptBuilder = None,
        response_parser: ResponseParser = None,
        urgency_scorer: UrgencyScorer = None,
        emotion_detector: EmotionDetector = None,
        action_generator: ActionGenerator = None,
        cache: CacheService = None,
    ) -> None:
        self.api_key = os.getenv("GEMINI_API_KEY", "").strip()
        self.model = os.getenv("GEMINI_MODEL", "gemini-2.0-flash").strip()
        self.use_mock = os.getenv("USE_MOCK_AI", "false").lower() == "true"
        
        # Dependencies injection
        self.cleaner = cleaner or TextCleaningService()
        self.ner = ner or NerService()
        self.knn = knn or KnnSeverityService(k=3)
        self.prompt_builder = prompt_builder or PromptBuilder()
        self.response_parser = response_parser or ResponseParser(self.ner)
        self.urgency_scorer = urgency_scorer or UrgencyScorer()
        self.emotion_detector = emotion_detector or EmotionDetector()
        self.action_generator = action_generator or ActionGenerator()
        self.cache = cache or CacheService()
        
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
        cache_key = self.cache.make_key(cleaned_post)
        
        cached_result = self.cache.get(cache_key)
        if cached_result:
            cached_result.raw["cache_hit"] = True
            return cached_result

        result = None
        if self.gemini_enabled:
            try:
                result = self._analyze_with_gemini(cleaned_post)
            except Exception as exc:
                logger.warning(f"GEMINI_FALLBACK | post_id={post.id} error={type(exc).__name__}: {exc}")
                result = self._analyze_with_mock(cleaned_post)
                result.raw["gemini_error"] = str(exc)
        else:
            result = self._analyze_with_mock(cleaned_post)
            
        result.raw["cache_hit"] = False
        self.cache.set(cache_key, result, ttl=self.cache.ttl)
        return result

    def _clean_post(self, post: SocialMediaPost) -> SocialMediaPost:
        return post.model_copy(
            update={
                "text": self.cleaner.clean(post.text),
                "comments": self.cleaner.clean_many(post.comments),
            }
        )

    def _analyze_with_gemini(self, post: SocialMediaPost) -> AnalysisResult:
        prompt = self.prompt_builder.build_prompt(post)
        response = self._client.models.generate_content(model=self.model, contents=prompt)
        text = getattr(response, "text", "") or ""
        payload = self.response_parser.extract_json(text)
        
        combined_text = self._combined_text(post)
        result = self.response_parser.result_from_payload(
            post, payload, source="gemini", combined_text=combined_text, raw={"gemini_text": text}
        )
        return self._apply_rule_based_urgency(post, result)

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
        dominant = self.emotion_detector.dominant_emotion(combined_text, base_negative)
        from schemas import EmotionLabel
        if dominant == EmotionLabel.JOY:
            base_negative = max(0.05, base_negative * 0.2)

        is_really_critical = self.urgency_scorer.is_really_critical(combined_text, need_categories)
        is_emergency = base_negative >= 0.35 and trigger_hits > 0 and is_really_critical
        rule_urgency = self.urgency_scorer.urgency_from_score(base_negative, need_categories, combined_text, trigger_hits)
        
        knn_result = self.knn.predict(post, trigger_hits, need_categories, base_negative)
        knn_urgency = self.response_parser._enum_or_default(UrgencyLevel, knn_result["predicted_urgency"], UrgencyLevel.LOW)
        urgency = self.urgency_scorer.max_urgency(rule_urgency, knn_urgency)
        
        if not is_emergency and urgency in {UrgencyLevel.CRITICAL, UrgencyLevel.HIGH}:
            urgency = UrgencyLevel.MEDIUM
            
        emotion_scores = self.emotion_detector.mock_emotion_scores(dominant, base_negative)

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
                affected_people_estimate=estimate_people_count(combined_text),
                recommended_action=self.action_generator.recommended_action(urgency, need_categories, locations),
            ),
            summary=self.action_generator.summary(post, urgency, need_categories, locations),
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
        knn_urgency = self.response_parser._enum_or_default(UrgencyLevel, knn_result["predicted_urgency"], UrgencyLevel.LOW)
        final_urgency = self.urgency_scorer.max_urgency(final_urgency, knn_urgency)
        
        is_really_critical = self.urgency_scorer.is_really_critical(combined_text, categories)
        is_emergency = result.humanitarian_signal.is_emergency
        
        if result.negative_score >= 0.5 and trigger_hits > 0 and is_really_critical:
            is_emergency = True
            final_urgency = self.urgency_scorer.urgency_from_score(
                result.negative_score,
                categories,
                combined_text,
                trigger_hits,
            )
        else:
            is_emergency = is_emergency and is_really_critical
            if not is_emergency:
                if final_urgency in {UrgencyLevel.CRITICAL, UrgencyLevel.HIGH}:
                    final_urgency = UrgencyLevel.MEDIUM

        result.humanitarian_signal.is_emergency = is_emergency
        result.humanitarian_signal.urgency = final_urgency
        result.humanitarian_signal.categories = categories or [NeedCategory.UNKNOWN]
        result.humanitarian_signal.locations = list(
            dict.fromkeys([*result.humanitarian_signal.locations, *locations])
        )
        result.humanitarian_signal.recommended_action = self.action_generator.recommended_action(
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
        return len(_TRIGGER_PATTERN.findall(text))

    def _reaction_pressure(self, reactions: dict[str, int]) -> float:
        sad = reactions.get("sad", 0) + reactions.get("care", 0)
        angry = reactions.get("angry", 0)
        total = sum(max(0, value) for value in reactions.values())
        if total == 0:
            return 0.0
        return min(0.25, ((sad + angry) / total) * 0.25)

def top_locations(results: list[AnalysisResult]) -> list[str]:
    counter: Counter[str] = Counter()
    for result in results:
        counter.update(result.humanitarian_signal.locations)
    return [location for location, _ in counter.most_common(5)]

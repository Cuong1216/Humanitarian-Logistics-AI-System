import asyncio
import os
import re
from collections import Counter
from concurrent.futures import ThreadPoolExecutor

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic_settings import BaseSettings
from schemas import (
    AnalyzeRequest,
    AnalysisResult,
    AreaPriorityResponse,
    AreaPriorityResult,
    BatchAnalysisResult,
    BatchAnalyzeRequest,
    EmotionLabel,
    HealthResponse,
    HumanitarianSignal,
    KeywordAnalyzeRequest,
    NeedCategory,
    SocialMediaPost,
    UrgencyLevel,
)
from services.categorization_service import CategorizationService
from services.nlp_service import NlpService, top_locations
from services.sentiment_service import SentimentService
from scoring_config import default_weights

app = FastAPI(
    title="Humanitarian Logistics AI Engine",
    description="Local AI engine for social-media emotion detection and humanitarian logistics signals.",
    version="1.0.0",
)

class Settings(BaseSettings):
    APP_ENV: str = "development"
    ALLOWED_ORIGINS: str = "http://localhost:3000,http://127.0.0.1:3000"
    
    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()

if settings.APP_ENV == "production" and settings.ALLOWED_ORIGINS == "*":
    raise RuntimeError("CORS configuration error: ALLOWED_ORIGINS=* is not safe for production.")

origins = [origin.strip() for origin in settings.ALLOWED_ORIGINS.split(",") if origin.strip()]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

nlp_service = NlpService()
sentiment_service = SentimentService()
categorization_service = CategorizationService()

MAX_CONCURRENT_GEMINI_CALLS = int(os.environ.get("MAX_CONCURRENT_GEMINI_CALLS", "5"))
executor = ThreadPoolExecutor(max_workers=MAX_CONCURRENT_GEMINI_CALLS)
_semaphore = None

def get_semaphore() -> asyncio.Semaphore:
    global _semaphore
    if _semaphore is None:
        _semaphore = asyncio.Semaphore(MAX_CONCURRENT_GEMINI_CALLS)
    return _semaphore

async def _analyze_post_async(post: SocialMediaPost) -> AnalysisResult:
    sem = get_semaphore()
    async with sem:
        loop = asyncio.get_running_loop()
        try:
            return await asyncio.wait_for(
                loop.run_in_executor(executor, nlp_service.analyze_post, post),
                timeout=30.0
            )
        except asyncio.TimeoutError:
            return AnalysisResult(
                post_id=post.id,
                keyword=post.keyword,
                dominant_emotion=EmotionLabel.NEUTRAL,
                emotion_scores=[],
                negative_score=0.0,
                confidence=0.0,
                humanitarian_signal=HumanitarianSignal(
                    is_emergency=False,
                    urgency=UrgencyLevel.LOW,
                    categories=[NeedCategory.UNKNOWN],
                    locations=[],
                    recommended_action="Không thể phân tích do timeout."
                ),
                summary="Timeout khi gọi AI service.",
                source="mock"
            )

URGENCY_SCORE = {
    UrgencyLevel.LOW: 0.25,
    UrgencyLevel.MEDIUM: 0.5,
    UrgencyLevel.HIGH: 0.75,
    UrgencyLevel.CRITICAL: 1.0,
}


@app.get("/", response_model=HealthResponse)
def root() -> HealthResponse:
    return health()


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        gemini_enabled=nlp_service.gemini_enabled,
        model=nlp_service.model,
    )


@app.post("/analyze", response_model=AnalysisResult)
async def analyze(request: AnalyzeRequest) -> AnalysisResult:
    return await _analyze_post_async(request.post)


@app.post("/analyze/batch", response_model=BatchAnalysisResult)
async def analyze_batch(request: BatchAnalyzeRequest) -> BatchAnalysisResult:
    tasks = [_analyze_post_async(post) for post in request.posts]
    results = await asyncio.gather(*tasks)
    return _build_batch_result(list(results))


@app.post("/analyze/keyword", response_model=BatchAnalysisResult)
async def analyze_keyword(request: KeywordAnalyzeRequest) -> BatchAnalysisResult:
    tasks = [_analyze_post_async(post) for post in _filter_posts_by_keyword(request)]
    results = await asyncio.gather(*tasks)
    return _build_batch_result(list(results))


@app.post("/analyze/areas", response_model=AreaPriorityResponse)
async def analyze_areas(request: BatchAnalyzeRequest) -> AreaPriorityResponse:
    tasks = [_analyze_post_async(post) for post in request.posts]
    results = await asyncio.gather(*tasks)
    return _build_area_priority_response(list(results))


@app.post("/analyze/keyword/areas", response_model=AreaPriorityResponse)
async def analyze_keyword_areas(request: KeywordAnalyzeRequest) -> AreaPriorityResponse:
    tasks = [_analyze_post_async(post) for post in _filter_posts_by_keyword(request)]
    results = await asyncio.gather(*tasks)
    return _build_area_priority_response(list(results))


def _filter_posts_by_keyword(request: KeywordAnalyzeRequest):
    keyword = request.keyword.lower()
    return [
        post
        for post in request.posts
        if keyword in post.text.lower()
        or (post.keyword is not None and keyword in post.keyword.lower())
        or any(keyword in comment.lower() for comment in post.comments)
    ]


def _build_batch_result(results: list[AnalysisResult]) -> BatchAnalysisResult:
    if not results:
        return BatchAnalysisResult(
            results=[],
            total_posts=0,
            emergency_posts=0,
            average_negative_score=0,
            most_urgent_level=UrgencyLevel.LOW,
            top_locations=[],
        )

    emergency_posts = sum(1 for result in results if result.humanitarian_signal.is_emergency)
    average_negative_score = round(
        sum(result.negative_score for result in results) / len(results),
        3,
    )

    return BatchAnalysisResult(
        results=results,
        total_posts=len(results),
        emergency_posts=emergency_posts,
        average_negative_score=average_negative_score,
        most_urgent_level=categorization_service.most_urgent(results),
        top_locations=top_locations(results),
    )


def _build_area_priority_response(results: list[AnalysisResult]) -> AreaPriorityResponse:
    groups: dict[str, list[tuple[AnalysisResult, float, int]]] = {}
    for result in results:
        locations = result.humanitarian_signal.locations or ["không rõ địa điểm"]
        for location in locations:
            boost = _location_context_boost(result, location)
            groups.setdefault(location, []).append((result, boost, len(locations)))

    area_rows: list[dict] = []
    for location, area_items in groups.items():
        area_results = [result for result, _, _ in area_items]
        post_scores = [_location_post_score(result, boost, location_count) for result, boost, location_count in area_items]
        average_score = sum(post_scores) / len(post_scores)
        max_score = max(post_scores)
        emergency_ratio = sum(
            1 for result in area_results if result.humanitarian_signal.is_emergency
        ) / len(area_results)
        context_score = max(boost for _, boost, _ in area_items)
        severity_score = round(min(1.0, average_score * default_weights.area_average_weight + max_score * default_weights.area_max_weight + emergency_ratio * default_weights.area_emergency_weight), 3)
        urgency = _urgency_from_area_score(severity_score)
        categories = _top_categories(area_results)

        area_rows.append(
            {
                "location": location,
                "post_count": len(area_results),
                "emergency_posts": sum(
                    1 for result in area_results if result.humanitarian_signal.is_emergency
                ),
                "severity_score": severity_score,
                "urgency": urgency,
                "categories": categories,
                "recommended_action": _area_recommended_action(location, urgency, categories),
                "post_ids": [result.post_id for result in area_results],
                "_context_score": context_score,
            }
        )

    area_rows.sort(key=lambda row: (row["severity_score"], row["_context_score"]), reverse=True)
    areas = []
    for index, row in enumerate(area_rows):
        row.pop("_context_score", None)
        areas.append(AreaPriorityResult(priority_rank=index + 1, **row))

    return AreaPriorityResponse(
        areas=areas,
        total_areas=len(areas),
        highest_priority_location=areas[0].location if areas else None,
        analyzed_posts=len(results),
        results=results,
    )


def _location_context_boost(result: AnalysisResult, location: str) -> float:
    text = str(result.raw.get("cleaned_text") or "").lower()
    target = location.lower()
    if target not in text:
        return 0.0

    high_terms = [
        "cần cứu hộ",
        "can cuu ho",
        "cần cứu",
        "can cuu",
        "cứu với",
        "cuu voi",
        "khẩn cấp",
        "khan cap",
        "mắc kẹt",
        "mac ket",
    ]
    medium_terms = [
        "thiếu nước",
        "thieu nuoc",
        "thiếu thuốc",
        "thieu thuoc",
        "thiếu lương thực",
        "thieu luong thuc",
        "bị ngập",
        "bi ngap",
        "ngập",
        "ngap",
    ]
    next_location_pattern = re.compile(
        r"\b(?:xã|xa|thôn|thon|làng|lang|huyện|huyen|tỉnh|tinh|phường|phuong|quận|quan|tp\.?|thành phố|thanh pho)\s+",
        flags=re.IGNORECASE,
    )

    best_boost = 0.0
    for match in re.finditer(re.escape(target), text, flags=re.IGNORECASE):
        start = match.start()
        end = match.end()
        next_match = next_location_pattern.search(text, end)
        window_end = next_match.start() if next_match else end + 120
        window = text[start:window_end]

        boost = 0.0
        if any(term in window for term in high_terms):
            boost += default_weights.high_term_boost
        if any(term in window for term in medium_terms):
            boost += default_weights.medium_term_boost
        best_boost = max(best_boost, min(boost, default_weights.max_context_boost))

    return best_boost


def _location_post_score(result: AnalysisResult, context_boost: float, location_count: int) -> float:
    base_score = _post_severity_score(result)
    if location_count <= 1:
        return min(1.0, base_score + context_boost)

    if context_boost >= default_weights.high_term_boost:
        return min(1.0, base_score + context_boost)
    if context_boost >= default_weights.medium_term_boost:
        return min(0.74, base_score * 0.65 + context_boost)
    return min(0.55, base_score * 0.55)

def _post_severity_score(result: AnalysisResult) -> float:
    rule_score = URGENCY_SCORE[result.humanitarian_signal.urgency]
    knn_payload = result.raw.get("knn_severity", {})
    knn_urgency = _enum_or_default(
        UrgencyLevel,
        knn_payload.get("predicted_urgency"),
        result.humanitarian_signal.urgency,
    )
    knn_score = URGENCY_SCORE[knn_urgency]
    knn_confidence = float(knn_payload.get("confidence", 0.0) or 0.0)
    return min(
        1.0,
        rule_score * default_weights.rule_weight
        + result.negative_score * default_weights.negative_weight
        + knn_score * default_weights.knn_weight
        + knn_confidence * default_weights.knn_confidence_weight,
    )


def _urgency_from_area_score(score: float) -> UrgencyLevel:
    if score >= default_weights.critical_threshold:
        return UrgencyLevel.CRITICAL
    if score >= default_weights.high_threshold:
        return UrgencyLevel.HIGH
    if score >= default_weights.medium_threshold:
        return UrgencyLevel.MEDIUM
    return UrgencyLevel.LOW


def _top_categories(results: list[AnalysisResult]) -> list[NeedCategory]:
    counter: Counter[NeedCategory] = Counter()
    for result in results:
        counter.update(result.humanitarian_signal.categories)
    categories = [category for category, _ in counter.most_common() if category != NeedCategory.UNKNOWN]
    return categories or [NeedCategory.UNKNOWN]


def _area_recommended_action(
    location: str,
    urgency: UrgencyLevel,
    categories: list[NeedCategory],
) -> str:
    needs = ", ".join(category.to_vietnamese() for category in categories)
    if urgency == UrgencyLevel.CRITICAL:
        return f"Ưu tiên cao nhất: điều phối cứu trợ {needs} đến {location} ngay lập tức."
    if urgency == UrgencyLevel.HIGH:
        return f"Ưu tiên sớm: chuẩn bị nguồn lực {needs} và xác minh tuyến đường đến {location}."
    if urgency == UrgencyLevel.MEDIUM:
        return f"Theo dõi sát {location}, chuẩn bị hỗ trợ {needs} nếu tín hiệu tiếp tục tăng."
    return f"Tiếp tục thu thập tín hiệu mạng xã hội tại {location}."





def _enum_or_default(enum_cls: type, value, default):
    try:
        return enum_cls(value)
    except ValueError:
        return default








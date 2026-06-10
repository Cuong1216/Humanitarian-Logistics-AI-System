from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from schemas import (
    AnalyzeRequest,
    AnalysisResult,
    BatchAnalysisResult,
    BatchAnalyzeRequest,
    HealthResponse,
    KeywordAnalyzeRequest,
    UrgencyLevel,
)
from services.categorization_service import CategorizationService
from services.nlp_service import NlpService, top_locations
from services.sentiment_service import SentimentService

app = FastAPI(
    title="KeyEmotion AI Engine",
    description="Local AI engine for social-media emotion detection and humanitarian logistics signals.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

nlp_service = NlpService()
sentiment_service = SentimentService()
categorization_service = CategorizationService()


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
def analyze(request: AnalyzeRequest) -> AnalysisResult:
    return nlp_service.analyze_post(request.post)


@app.post("/analyze/batch", response_model=BatchAnalysisResult)
def analyze_batch(request: BatchAnalyzeRequest) -> BatchAnalysisResult:
    return _build_batch_result([nlp_service.analyze_post(post) for post in request.posts])


@app.post("/analyze/keyword", response_model=BatchAnalysisResult)
def analyze_keyword(request: KeywordAnalyzeRequest) -> BatchAnalysisResult:
    keyword = request.keyword.lower()
    matched_posts = [
        post
        for post in request.posts
        if keyword in post.text.lower()
        or (post.keyword is not None and keyword in post.keyword.lower())
        or any(keyword in comment.lower() for comment in post.comments)
    ]
    results = [nlp_service.analyze_post(post) for post in matched_posts]
    return _build_batch_result(results)


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

from enum import Enum
from typing import Any

from pydantic import BaseModel, Field, HttpUrl, field_validator


class PlatformName(str, Enum):
    FACEBOOK = "facebook"
    X = "x"
    OTHER = "other"


class EmotionLabel(str, Enum):
    ANGER = "anger"
    FEAR = "fear"
    SADNESS = "sadness"
    DISGUST = "disgust"
    JOY = "joy"
    NEUTRAL = "neutral"
    MIXED = "mixed"


class UrgencyLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


class NeedCategory(str, Enum):
    FOOD = "food"
    WATER = "water"
    MEDICAL = "medical"
    SHELTER = "shelter"
    RESCUE = "rescue"
    TRANSPORT = "transport"
    SANITATION = "sanitation"
    UNKNOWN = "unknown"


class SocialMediaPost(BaseModel):
    id: str = Field(..., examples=["post-001"])
    platform: PlatformName = PlatformName.OTHER
    author: str | None = None
    text: str = Field(..., min_length=1)
    keyword: str | None = Field(default=None, examples=["Yagi storm"])
    location_hint: str | None = Field(default=None, examples=["village XYZ"])
    reactions: dict[str, int] = Field(default_factory=dict)
    comments: list[str] = Field(default_factory=list)
    shares: int = 0
    url: str | None = None
    created_at: str | None = None


class AnalyzeRequest(BaseModel):
    post: SocialMediaPost


class BatchAnalyzeRequest(BaseModel):
    posts: list[SocialMediaPost] = Field(..., min_length=1)


class KeywordAnalyzeRequest(BaseModel):
    keyword: str = Field(..., min_length=1)
    posts: list[SocialMediaPost] = Field(..., min_length=1)

    @field_validator("posts", mode="before")
    @classmethod
    def accept_single_post(cls, value: Any) -> Any:
        if isinstance(value, dict):
            return [value]
        return value


class EmotionScore(BaseModel):
    label: EmotionLabel
    score: float = Field(..., ge=0, le=1)


class HumanitarianSignal(BaseModel):
    is_emergency: bool
    urgency: UrgencyLevel
    categories: list[NeedCategory]
    locations: list[str]
    affected_people_estimate: int | None = None
    recommended_action: str


class AnalysisResult(BaseModel):
    post_id: str
    keyword: str | None
    dominant_emotion: EmotionLabel
    emotion_scores: list[EmotionScore]
    negative_score: float = Field(..., ge=0, le=1)
    confidence: float = Field(..., ge=0, le=1)
    humanitarian_signal: HumanitarianSignal
    summary: str
    source: str = Field(..., description="gemini or mock")
    raw: dict[str, Any] = Field(default_factory=dict)


class BatchAnalysisResult(BaseModel):
    results: list[AnalysisResult]
    total_posts: int
    emergency_posts: int
    average_negative_score: float
    most_urgent_level: UrgencyLevel
    top_locations: list[str]


class HealthResponse(BaseModel):
    status: str
    gemini_enabled: bool
    model: str


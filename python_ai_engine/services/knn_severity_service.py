import math
from dataclasses import dataclass

from schemas import NeedCategory, SocialMediaPost, UrgencyLevel
from services.text_utils import estimate_people_count


@dataclass(frozen=True)
class KnnSample:
    features: list[float]
    label: UrgencyLevel


class KnnSeverityService:
    """KNN classifier for local post severity prediction.

    The model uses hand-crafted numeric features extracted from a social post.
    This keeps the algorithm easy to explain in reports while still being a
    real ML-style nearest-neighbor classifier.
    """

    def __init__(self, k: int = 3) -> None:
        self.k = k
        self.training_samples = self._build_training_samples()

    def predict(
        self,
        post: SocialMediaPost,
        trigger_hits: int,
        categories: list[NeedCategory],
        negative_score: float,
    ) -> dict:
        features = self.extract_features(post, trigger_hits, categories, negative_score)
        neighbors = sorted(
            (
                (self._euclidean_distance(features, sample.features), sample.label)
                for sample in self.training_samples
            ),
            key=lambda item: item[0],
        )[: self.k]

        votes: dict[UrgencyLevel, float] = {}
        for distance, label in neighbors:
            weight = 1 / (distance + 1e-6)
            votes[label] = votes.get(label, 0.0) + weight

        label = max(votes, key=votes.get)
        confidence = votes[label] / sum(votes.values()) if votes else 0.0
        return {
            "algorithm": "KNN",
            "k": self.k,
            "predicted_urgency": label.value,
            "confidence": round(confidence, 3),
            "features": [round(value, 3) for value in features],
            "nearest_neighbors": [
                {"distance": round(distance, 3), "label": label.value}
                for distance, label in neighbors
            ],
        }

    def extract_features(
        self,
        post: SocialMediaPost,
        trigger_hits: int,
        categories: list[NeedCategory],
        negative_score: float,
    ) -> list[float]:
        reactions = post.reactions or {}
        text = " ".join([post.text, *post.comments]).lower()
        people_estimate = estimate_people_count(text, default=0)
        sad_care = reactions.get("sad", 0) + reactions.get("care", 0)
        angry = reactions.get("angry", 0)

        return [
            min(trigger_hits / 10, 1),
            1.0 if NeedCategory.RESCUE in categories else 0.0,
            1.0 if NeedCategory.MEDICAL in categories else 0.0,
            1.0 if NeedCategory.FOOD in categories else 0.0,
            1.0 if NeedCategory.WATER in categories else 0.0,
            1.0 if NeedCategory.TRANSPORT in categories else 0.0,
            min(len(post.comments) / 10, 1),
            min(sad_care / 200, 1),
            min(angry / 100, 1),
            min(post.shares / 100, 1),
            min(people_estimate / 500, 1),
            max(0.0, min(negative_score, 1.0)),
        ]

    def _build_training_samples(self) -> list[KnnSample]:
        return [
            KnnSample([0.0, 0, 0, 0, 0, 0, 0.0, 0.02, 0.0, 0.0, 0.0, 0.05], UrgencyLevel.LOW),
            KnnSample([0.1, 0, 0, 1, 0, 0, 0.1, 0.08, 0.02, 0.05, 0.02, 0.25], UrgencyLevel.LOW),
            KnnSample([0.25, 0, 0, 1, 1, 0, 0.2, 0.2, 0.08, 0.15, 0.1, 0.4], UrgencyLevel.MEDIUM),
            KnnSample([0.35, 0, 1, 0, 1, 0, 0.3, 0.3, 0.1, 0.25, 0.16, 0.55], UrgencyLevel.MEDIUM),
            KnnSample([0.5, 0, 1, 1, 1, 1, 0.4, 0.45, 0.22, 0.45, 0.3, 0.68], UrgencyLevel.HIGH),
            KnnSample([0.6, 1, 0, 1, 1, 1, 0.5, 0.55, 0.25, 0.6, 0.4, 0.75], UrgencyLevel.HIGH),
            KnnSample([0.75, 1, 1, 1, 1, 1, 0.7, 0.75, 0.45, 0.8, 0.6, 0.88], UrgencyLevel.CRITICAL),
            KnnSample([0.9, 1, 1, 1, 1, 1, 0.9, 1.0, 0.8, 1.0, 1.0, 1.0], UrgencyLevel.CRITICAL),
        ]

    def _euclidean_distance(self, first: list[float], second: list[float]) -> float:
        return math.sqrt(sum((a - b) ** 2 for a, b in zip(first, second)))

from collections import Counter

from schemas import AnalysisResult, NeedCategory, UrgencyLevel


class CategorizationService:
    URGENCY_ORDER = {
        UrgencyLevel.LOW: 0,
        UrgencyLevel.MEDIUM: 1,
        UrgencyLevel.HIGH: 2,
        UrgencyLevel.CRITICAL: 3,
    }

    def most_urgent(self, results: list[AnalysisResult]) -> UrgencyLevel:
        if not results:
            return UrgencyLevel.LOW
        return max(
            (result.humanitarian_signal.urgency for result in results),
            key=lambda urgency: self.URGENCY_ORDER[urgency],
        )

    def top_categories(self, results: list[AnalysisResult]) -> list[NeedCategory]:
        counter: Counter[NeedCategory] = Counter()
        for result in results:
            counter.update(result.humanitarian_signal.categories)
        return [category for category, _ in counter.most_common()]

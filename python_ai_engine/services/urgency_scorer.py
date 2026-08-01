from schemas import NeedCategory, UrgencyLevel

class UrgencyScorer:
    """
    Xử lý các logic chấm điểm khẩn cấp rule-based.
    """

    def is_really_critical(self, text: str, categories: list[NeedCategory]) -> bool:
        # Check for separation, isolation, or loss of survival elements
        isolation_keywords = [
            "cô lập", "co lap", "chia cắt", "chia cat", 
            "mất điện", "mat dien", "mất nước", "mat nuoc", "mất nước sạch", "mat nuoc sach",
            "mất liên lạc", "mat lien lac", "ngập sâu", "ngap sau", "ngập mái", "ngap mai",
            "mắc kẹt", "mac ket", "bị kẹt", "bi ket",
            "cuu voi", "cứu với", "cần cứu", "can cuu", "cứu nạn", "cuu nan", "cứu hộ", "cuu ho"
        ]
        text_lower = text.lower()
        has_isolation = any(kw in text_lower for kw in isolation_keywords)
        
        # Check for basic survival need categories
        survival_categories = {
            NeedCategory.FOOD,
            NeedCategory.WATER,
            NeedCategory.MEDICAL,
            NeedCategory.RESCUE,
            NeedCategory.SHELTER
        }
        has_survival_need = any(cat in survival_categories for cat in categories)
        
        return has_isolation or has_survival_need

    def urgency_from_score(
        self,
        score: float,
        categories: list[NeedCategory],
        text: str,
        trigger_hits: int,
    ) -> UrgencyLevel:
        if not self.is_really_critical(text, categories):
            if score >= 0.6:
                return UrgencyLevel.MEDIUM
            return UrgencyLevel.LOW

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

    def max_urgency(self, first: UrgencyLevel, second: UrgencyLevel) -> UrgencyLevel:
        order = {
            UrgencyLevel.LOW: 0,
            UrgencyLevel.MEDIUM: 1,
            UrgencyLevel.HIGH: 2,
            UrgencyLevel.CRITICAL: 3,
        }
        return first if order[first] >= order[second] else second

from schemas import NeedCategory, UrgencyLevel, SocialMediaPost

class ActionGenerator:
    """
    Chịu trách nhiệm tạo text hướng dẫn (recommended action) và tóm tắt (summary).
    """

    def recommended_action(
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

    def summary(
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

import json

from schemas import SocialMediaPost


class PromptBuilder:
    """
    Xây dựng prompt (Prompt Engineering) để gửi cho mô hình LLM.
    """
    
    def build_prompt(self, post: SocialMediaPost) -> str:
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
- CHỈ đánh dấu khẩn cấp ("is_emergency": true) khi bài viết ghi nhận vấn đề nghiêm trọng thực tế (người dân bị chia cắt, cô lập, lũ cuốn, mắc kẹt nguy hiểm hoặc thiếu thốn trầm trọng các nhu yếu phẩm sinh tồn cơ bản như lương thực, nước sạch, thuốc men, nơi trú ẩn).
- Các tin tức chung, dự báo thời tiết, cảnh báo chung mà không có thông tin thiệt hại về người hoặc nhu cầu cứu trợ sinh tồn cụ thể thì phải đặt "is_emergency": false và "urgency" ở mức "low" hoặc "medium".
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

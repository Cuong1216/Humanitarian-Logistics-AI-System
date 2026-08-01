"""Shared text utility functions dùng chung cho các services."""
import re


def estimate_people_count(text: str, default: int | None = None) -> int | None:
    """
    Ước tính số người bị ảnh hưởng từ text bằng cách tìm số nguyên lớn nhất.
    
    Args:
        text: Văn bản cần phân tích.
        default: Giá trị trả về nếu không tìm thấy số nào.
    
    Returns:
        Số nguyên lớn nhất tìm được trong văn bản, hoặc `default`.
    """
    numbers = [int(item) for item in re.findall(r"\b\d{1,6}\b", text)]
    return max(numbers) if numbers else default

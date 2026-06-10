import re

from schemas import NeedCategory


NEED_KEYWORDS = {
    NeedCategory.FOOD: {
        "an uong",
        "ăn uống",
        "com",
        "cơm",
        "do an",
        "đồ ăn",
        "doi",
        "đói",
        "gao",
        "gạo",
        "luong kho",
        "lương khô",
        "luong thuc",
        "lương thực",
        "mi goi",
        "mì gói",
        "thuc pham",
        "thực phẩm",
    },
    NeedCategory.WATER: {
        "khát",
        "khat",
        "nuoc",
        "nước",
        "nuoc sach",
        "nước sạch",
        "nuoc uong",
        "nước uống",
        "thieu nuoc",
        "thiếu nước",
    },
    NeedCategory.MEDICAL: {
        "bac si",
        "bác sĩ",
        "benh",
        "bệnh",
        "bi thuong",
        "bị thương",
        "cap cuu",
        "cấp cứu",
        "cuu thuong",
        "cứu thương",
        "thuoc",
        "thuốc",
        "y te",
        "y tế",
    },
    NeedCategory.SHELTER: {
        "cho o",
        "chỗ ở",
        "leu",
        "lều",
        "mat nha",
        "mất nhà",
        "nha sap",
        "nhà sập",
        "noi tru an",
        "nơi trú ẩn",
        "tam tru",
        "tạm trú",
        "tru an",
        "trú ẩn",
    },
    NeedCategory.RESCUE: {
        "can cuu",
        "cần cứu",
        "cuu ho",
        "cứu hộ",
        "cuu nan",
        "cứu nạn",
        "cuu voi",
        "cứu với",
        "di tan",
        "di tản",
        "mac ket",
        "mắc kẹt",
        "mat tich",
        "mất tích",
        "so tan",
        "sơ tán",
    },
    NeedCategory.TRANSPORT: {
        "cau sap",
        "cầu sập",
        "duong bi cat",
        "đường bị cắt",
        "duong hu",
        "đường hư",
        "ket duong",
        "kẹt đường",
        "van chuyen",
        "vận chuyển",
        "xe cuu tro",
        "xe cứu trợ",
        "xe tai",
        "xe tải",
    },
    NeedCategory.SANITATION: {
        "dich benh",
        "dịch bệnh",
        "nuoc ban",
        "nước bẩn",
        "o nhiem",
        "ô nhiễm",
        "rac thai",
        "rác thải",
        "ve sinh",
        "vệ sinh",
    },
}


class NerService:
    """Regex/keyword fallback khi không gọi được Gemini extraction."""

    LOCATION_PATTERNS = [
        re.compile(
            r"\b(?:xã|xa|thôn|thon|làng|lang|huyện|huyen|tỉnh|tinh|phường|phuong|quận|quan|tp\.?|thành phố|thanh pho)\s+[\wÀ-ỹ-]+(?:\s+[\wÀ-ỹ-]+)*",
            flags=re.IGNORECASE,
        ),
        re.compile(
            r"\b[\wÀ-ỹ-]+(?:\s+[\wÀ-ỹ-]+)*\s+(?:xã|xa|thôn|thon|làng|lang|huyện|huyen|tỉnh|tinh|phường|phuong|quận|quan)\b",
            flags=re.IGNORECASE,
        ),
    ]

    def extract_locations(self, text: str, location_hint: str | None = None) -> list[str]:
        locations: list[str] = []
        if location_hint:
            locations.append(location_hint)

        for pattern in self.LOCATION_PATTERNS:
            locations.extend(pattern.findall(text))

        cleaned = [self._clean_location(location) for location in locations]
        return self._dedupe_locations(cleaned)

    def extract_needs(self, text: str) -> list[NeedCategory]:
        lowered = text.lower()
        detected: list[NeedCategory] = []
        for category, keywords in NEED_KEYWORDS.items():
            if any(keyword in lowered for keyword in keywords):
                detected.append(category)
        return detected

    def _clean_location(self, location: str) -> str:
        location = location.strip(" .,;:")
        location = re.split(
            r"\s+(?:bị|bi|đang|dang|cần|can|thiếu|thieu|ngập|ngap|sập|sap|hư|hu|mất|mat)\b",
            location,
            maxsplit=1,
            flags=re.IGNORECASE,
        )[0]
        return location.strip(" .,;:")

    def _dedupe_locations(self, locations: list[str]) -> list[str]:
        result: list[str] = []
        for location in sorted((item for item in locations if item), key=len, reverse=True):
            lowered = location.lower()
            if any(lowered == kept.lower() or lowered in kept.lower() for kept in result):
                continue
            result.append(location)
        return result

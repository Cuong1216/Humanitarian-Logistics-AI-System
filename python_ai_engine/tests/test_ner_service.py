from services.ner_service import NerService
from schemas import NeedCategory

def test_extract_locations():
    service = NerService()
    result = service.extract_locations("xã Bình Minh bị ngập, người dân khổ quá")
    assert "xã Bình Minh" in result

def test_extract_locations_with_hint():
    service = NerService()
    result = service.extract_locations("nước dâng cao", location_hint="huyện Kim Sơn")
    assert "huyện Kim Sơn" in result

def test_dedupe_locations():
    service = NerService()
    result = service.extract_locations("xã Bình Minh bị ngập, tại xã Bình Minh")
    assert len(result) == 1
    assert result[0] == "xã Bình Minh"

def test_extract_needs():
    service = NerService()
    result = service.extract_needs("chúng tôi đang thiếu nước, cần thuốc men gấp")
    assert NeedCategory.WATER in result
    assert NeedCategory.MEDICAL in result

def test_empty_text():
    service = NerService()
    assert service.extract_locations("") == []
    assert service.extract_needs("") == []

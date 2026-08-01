from services.text_cleaning_service import TextCleaningService


def test_clean_url():
    service = TextCleaningService()
    result = service.clean("check http://example.com here")
    assert result == "check here"

def test_clean_mention():
    service = TextCleaningService()
    result = service.clean("@user123 xin chào")
    assert result == "xin chào"

def test_unicode_normalization():
    service = TextCleaningService()
    nfd_str = "xin cha\u0300o" # NFD for chào
    result = service.clean(nfd_str)
    assert result == "xin chào" # NFC

def test_clean_empty_and_none():
    service = TextCleaningService()
    assert service.clean("") == ""
    assert service.clean(None) == ""

def test_clean_many():
    service = TextCleaningService()
    texts = ["hello http://abc.com", "", "   ", "@test hi"]
    result = service.clean_many(texts)
    assert result == ["hello", "hi"]

from services.text_utils import estimate_people_count


def test_estimate_people_found():
    assert estimate_people_count("có 150 người bị ảnh hưởng") == 150

def test_estimate_people_not_found():
    assert estimate_people_count("không có số nào") is None
    assert estimate_people_count("không có số nào", default=0) == 0

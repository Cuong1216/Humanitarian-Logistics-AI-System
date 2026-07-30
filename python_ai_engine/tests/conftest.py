import pytest
from schemas import SocialMediaPost, PlatformName
from services.nlp_service import NlpService

@pytest.fixture
def sample_post():
    return SocialMediaPost(
        id="post-test-1",
        platform=PlatformName.FACEBOOK,
        text="Mình cần giúp đỡ",
        url="http://example.com"
    )

@pytest.fixture
def sample_emergency_post():
    return SocialMediaPost(
        id="post-test-2",
        platform=PlatformName.FACEBOOK,
        text="Lũ lụt dâng cao, nhà bị ngập, người dân xã Bình Minh mắc kẹt cần cứu hộ khẩn cấp",
        url="http://example.com/2"
    )

@pytest.fixture
def mock_nlp_service(monkeypatch):
    monkeypatch.setenv("USE_MOCK_AI", "true")
    monkeypatch.setenv("GEMINI_API_KEY", "")
    return NlpService()

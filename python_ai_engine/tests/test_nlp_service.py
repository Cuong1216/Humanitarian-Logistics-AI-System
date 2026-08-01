from unittest.mock import patch

from schemas import AnalysisResult
from services.nlp_service import NlpService


def test_mock_path(mock_nlp_service, sample_post):
    assert mock_nlp_service.gemini_enabled is False
    with patch.object(mock_nlp_service, '_analyze_with_gemini') as mock_gemini:
        result = mock_nlp_service.analyze_post(sample_post)
        mock_gemini.assert_not_called()
        assert isinstance(result, AnalysisResult)
        assert result.source == "mock"

def test_fallback_when_gemini_fails(monkeypatch, sample_post):
    monkeypatch.setenv("USE_MOCK_AI", "false")
    monkeypatch.setenv("GEMINI_API_KEY", "fake_key")
    
    service = NlpService()
    assert service.gemini_enabled is True
    
    with patch.object(service, '_analyze_with_gemini', side_effect=Exception("API Error")):
        result = service.analyze_post(sample_post)
        assert result.source == "mock"
        assert "gemini_error" in result.raw
        assert result.raw["gemini_error"] == "API Error"

def test_count_trigger_hits(mock_nlp_service):
    text = "lũ lụt dâng cao, nhiều người mắc kẹt cần cứu hộ khẩn cấp"
    count = mock_nlp_service._count_trigger_hits(text)
    # The text contains triggers like "lũ lụt", "mắc kẹt", "cứu hộ", "khẩn cấp"
    assert count >= 4

def test_count_trigger_hits_performance(mock_nlp_service):
    """Đảm bảo pre-compiled pattern hoạt động đúng."""
    text = "lũ lụt dâng cao, cần cứu hộ khẩn cấp, người dân mắc kẹt"
    hits = mock_nlp_service._count_trigger_hits(text)
    assert hits >= 3  # "lũ lụt", "cứu hộ", "mắc kẹt"

def test_count_trigger_hits_no_false_positive(mock_nlp_service):
    """Không trigger trên text bình thường."""
    text = "hôm nay trời nắng đẹp, bầu trời trong xanh"
    hits = mock_nlp_service._count_trigger_hits(text)
    assert hits == 0

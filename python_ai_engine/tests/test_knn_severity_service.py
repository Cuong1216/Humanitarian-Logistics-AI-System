from schemas import NeedCategory, UrgencyLevel
from services.knn_severity_service import KnnSeverityService


def test_predict_low(sample_post):
    service = KnnSeverityService(k=3)
    result = service.predict(sample_post, trigger_hits=0, categories=[], negative_score=0.0)
    assert result["predicted_urgency"] == UrgencyLevel.LOW.value

def test_predict_critical(sample_post):
    service = KnnSeverityService(k=3)
    critical_post = sample_post.model_copy(update={
        "reactions": {"sad": 100, "angry": 100},
        "shares": 100,
        "comments": ["help"] * 10,
        "text": "500 người đang gặp nguy hiểm"
    })
    result = service.predict(
        critical_post,
        trigger_hits=10,
        categories=[NeedCategory.RESCUE, NeedCategory.MEDICAL, NeedCategory.FOOD, NeedCategory.WATER, NeedCategory.TRANSPORT],
        negative_score=1.0
    )
    assert result["predicted_urgency"] == UrgencyLevel.CRITICAL.value

def test_confidence(sample_post):
    service = KnnSeverityService(k=3)
    result = service.predict(sample_post, trigger_hits=2, categories=[NeedCategory.FOOD], negative_score=0.5)
    assert 0 <= result["confidence"] <= 1.0

def test_k_1_vs_k_3(sample_post):
    service_k1 = KnnSeverityService(k=1)
    service_k3 = KnnSeverityService(k=3)
    
    res1 = service_k1.predict(sample_post, trigger_hits=2, categories=[NeedCategory.FOOD], negative_score=0.5)
    res3 = service_k3.predict(sample_post, trigger_hits=2, categories=[NeedCategory.FOOD], negative_score=0.5)
    
    assert res1["k"] == 1
    assert res3["k"] == 3

import pytest
from fastapi.testclient import TestClient
import os
from main import app

@pytest.fixture(autouse=True)
def setup_env(monkeypatch):
    monkeypatch.setenv("USE_MOCK_AI", "true")
    monkeypatch.setenv("GEMINI_API_KEY", "")

client = TestClient(app)

def test_get_health():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "gemini_enabled" in data

def test_post_analyze():
    payload = {
        "post": {
            "id": "post-123",
            "text": "Nhà tôi ở xã Bình Minh bị ngập nặng",
            "platform": "facebook"
        }
    }
    response = client.post("/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "post_id" in data
    assert data["post_id"] == "post-123"

def test_post_analyze_batch_empty():
    payload = {
        "posts": []
    }
    response = client.post("/analyze/batch", json=payload)
    assert response.status_code == 422

def test_post_analyze_keyword():
    payload = {
        "keyword": "lũ",
        "posts": [
            {"id": "p1", "text": "lũ lụt quá", "platform": "facebook"},
            {"id": "p2", "text": "trời nắng đẹp", "platform": "facebook"}
        ]
    }
    response = client.post("/analyze/keyword", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["total_posts"] == 1
    assert data["results"][0]["post_id"] == "p1"

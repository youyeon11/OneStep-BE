"""
헬스체크 API 테스트
"""
import pytest

def test_health_check(client):
    """헬스체크 엔드포인트 테스트"""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "timestamp" in data
    assert data["service"] == "FastAPI AI Server"

def test_readiness_check(client):
    """준비 상태 확인 엔드포인트 테스트"""
    response = client.get("/health/ready")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ready"
    assert "checks" in data
    assert "mongodb" in data["checks"]
    assert "kafka" in data["checks"]

def test_root_endpoint(client):
    """루트 엔드포인트 테스트"""
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["service"] == "OneStep FastAPI AI Server"
    assert data["version"] == "0.1.0"
    assert data["status"] == "running"

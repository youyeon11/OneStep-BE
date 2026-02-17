"""
pytest 설정 및 공통 fixture
"""
import pytest


@pytest.fixture
def api_key():
    """테스트용 API 키"""
    return "test-api-key"


@pytest.fixture
def auth_headers(api_key):
    """인증 헤더"""
    return {
        "Authorization": f"Bearer {api_key}"
    }

from enum import Enum
from typing import Tuple

class ErrorCode(Enum):
    """
    에러 코드 정의
    
    Spring의 ErrorCode 패턴을 따릅니다.
    (HTTP Status Code, Error Code, Error Message)
    """
    
    # JWT 토큰
    TOKEN_INVALID = (401, "TOKEN001", "토큰이 유효하지 않습니다.")
    TOKEN_SIGNATURE_INVALID = (401, "TOKEN002", "토큰 서명이 유효하지 않습니다.")
    TOKEN_EXPIRED = (401, "TOKEN003", "토큰 기한이 유효하지 않습니다.")
    TOKEN_UNSUPPORTED = (401, "TOKEN004", "지원하지 않는 토큰입니다.")
    TOKEN_NOT_FOUND = (401, "TOKEN005", "토큰이 비어있습니다.")
    
    # 인증/인가 (Auth)
    UNAUTHORIZED = (401, "UNAUTHORIZED", "인증되지 않은 접근입니다.")
    ACCESS_DENIED = (403, "ACCESS_DENIED", "허용되지 않은 접근 권한입니다.")
    
    # 사용자 (USER)
    USER_NOT_FOUND = (404, "USER001", "사용자를 찾을 수 없습니다.")
    INSUFFICIENT_DATA = (400, "USER002", "사용자 데이터가 부족합니다.")
    
    # 챌린지 (CHALLENGE)
    NO_AVAILABLE_CHALLENGES = (404, "CHALLENGE001", "추천 가능한 챌린지가 없습니다.")
    INVALID_RECOVERY_LEVEL = (400, "CHALLENGE002", "유효하지 않은 회복력 수준입니다 (1~3).")
    
    # 목표 (GOAL)
    INVALID_GOAL = (400, "GOAL001", "유효하지 않은 목표입니다.")
    GOAL_ANALYSIS_FAILED = (500, "GOAL002", "목표 분석에 실패했습니다.")
    
    # 대화/컨텍스트 (CONTEXT)
    INSUFFICIENT_CONTEXT = (400, "CONTEXT001", "대화 컨텍스트가 부족합니다.")
    
    # 채팅 (CHAT)
    CHAT_SESSION_NOT_FOUND = (404, "CHAT001", "채팅 세션을 찾을 수 없습니다.")
    CHAT_SESSION_EXPIRED = (410, "CHAT002", "채팅 세션이 만료되었습니다.")
    CHAT_MESSAGE_EMPTY = (400, "CHAT003", "메시지가 비어있습니다.")
    
    # 서버 (SERVER)
    INTERNAL_SERVER_ERROR = (500, "SERVER001", "서버 내부 오류가 발생했습니다.")
    MONGODB_CONNECTION_ERROR = (503, "SERVER002", "MongoDB 연결에 실패했습니다.")
    KAFKA_CONNECTION_ERROR = (503, "SERVER003", "Kafka 연결에 실패했습니다.")
    
    def __init__(self, status_code: int, error_code: str, message: str):
        self.status_code = status_code
        self.error_code = error_code
        self.message = message
    
    def to_dict(self) -> dict:
        """에러 정보를 딕셔너리로 변환"""
        return {
            "status_code": self.status_code,
            "error_code": self.error_code,
            "message": self.message
        }

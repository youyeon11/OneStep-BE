from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


# MongoDB에서 유저 프로필 문서
class UserProfileDocument(BaseModel):

    # 유저 코드
    user_code: str = Field(..., min_length=1, alias="userCode")
    
    # 유저 컨텍스트
    recovery_level: int = Field(..., ge=1, le=3, alias="recoveryLevel")
    pet_nickname: Optional[str] = Field(None, alias="petNickname")
    survey_tags: List[str] = Field(default_factory=list, alias="surveyTags")
    interest_tags: List[str] = Field(default_factory=list, alias="interestTags")
    
    # 챌린지 히스토리 요약 (챗봇 개인화용)
    completed_challenge_count: int = Field(default=0, alias="completedChallengeCount")
    avg_emotion_score: Optional[float] = Field(None, alias="avgEmotionScore")  # 평균 emotion (1~5)
    preferred_categories: List[str] = Field(default_factory=list, alias="preferredCategories")  # 선호 카테고리 상위 2개
    recent_challenge_ids: List[int] = Field(default_factory=list, alias="recentChallengeIds")  # 최근 완료 챌린지 5개
    
    # 메타데이터
    updated_at: datetime = Field(default_factory=datetime.now, alias="updatedAt")
    batch_id: str = Field(..., alias="batchId")
    
    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "userCode": "USER_ABC123",
                "recoveryLevel": 2,
                "surveyTags": ["LIFESTYLE", "INNER"],
                "interestTags": ["운동", "명상", "산책"],
                "completedChallengeCount": 15,
                "avgEmotionScore": 4.2,
                "preferredCategories": ["LIFESTYLE", "INNER"],
                "recentChallengeIds": [101, 45, 67, 89, 23],
                "updatedAt": "2026-01-28T23:15:00Z",
                "batchId": "550e8400-e29b-41d4-a716-446655440000"
            }
        }

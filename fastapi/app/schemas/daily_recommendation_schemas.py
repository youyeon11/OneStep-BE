from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


# 일일 챌린지 추천 API 스키마
class UserContext(BaseModel):
    recovery_level: Optional[int] = Field(None, ge=1, le=3, alias="recoveryLevel")

    class Config:
        populate_by_name = True


class ChallengeHistoryItem(BaseModel):
    challenge_id: Optional[int] = Field(None, alias="challengeId")
    challenge_master_id: Optional[int] = Field(None, alias="challengeMasterId")
    challenge_status: Optional[str] = Field(None, alias="challengeStatus")
    assigned_date: Optional[str] = Field(None, alias="assignedDate")
    completed_at: Optional[str] = Field(None, alias="completedAt")
    emotion: Optional[int] = Field(None, ge=1, le=5)
    origin: Optional[str] = Field(None)
    weight: Optional[float] = Field(None)

    class Config:
        populate_by_name = True


class RouteInfo(BaseModel):
    route_session_id: Optional[int] = Field(None, alias="routeSessionId")
    content: Optional[str] = None
    completed_at: Optional[str] = Field(None, alias="completedAt")
    challenge_status: Optional[str] = Field(None, alias="challengeStatus")
    origin: Optional[str] = Field(None)

    class Config:
        populate_by_name = True


class UserRecommendationData(BaseModel):
    user_code: str = Field(..., min_length=1, alias="userCode")
    request_date: str = Field(..., alias="requestDate")
    user_context: Optional[UserContext] = Field(None, alias="userContext")
    challenge_history: Optional[List[ChallengeHistoryItem]] = Field(default_factory=list, alias="challengeHistory")
    route: Optional[RouteInfo] = None

    class Config:
        populate_by_name = True


class DailyRecommendationBatchMessage(BaseModel):
    batch_id: Optional[str] = Field(None, alias="batchId")
    triggered_at: Optional[datetime] = Field(None, alias="triggeredAt")
    users: List[UserRecommendationData] = Field(default_factory=list)
    
    class Config:
        populate_by_name = True


# MongoDB Document Models
class RecommendationItem(BaseModel):
    challenge_code: int = Field(..., alias="challengeCode")
    weight: float = Field(..., ge=0.0, le=1.0)
    
    class Config:
        populate_by_name = True


class RecommendationMetadata(BaseModel):
    generated_at: datetime = Field(default_factory=datetime.now, alias="generatedAt")
    
    class Config:
        populate_by_name = True


class CreatedAtInfo(BaseModel):
    date: datetime
    
    class Config:
        populate_by_name = True


class DailyRecommendationDocument(BaseModel):
    user_code: str = Field(..., alias="userCode")
    recommendations: List[RecommendationItem]
    route: Optional[RouteInfo] = None
    metadata: RecommendationMetadata = Field(default_factory=RecommendationMetadata)
    created_at: CreatedAtInfo = Field(..., alias="createdAt")
    
    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "userCode": "U1001",
                "recommendations": [
                    {"challengeCode": 101, "weight": 0.95},
                    {"challengeCode": 130, "weight": 0.88},
                    {"challengeCode": 45, "weight": 0.82},
                    {"challengeCode": 67, "weight": 0.78},
                    {"challengeCode": 23, "weight": 0.71}
                ],
                "createdAt": {
                    "date": "2026-01-27T08:14:15.659Z"
                }
            }
        }

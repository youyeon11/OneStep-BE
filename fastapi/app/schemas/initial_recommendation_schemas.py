from pydantic import BaseModel, Field, field_validator
from typing import List
from datetime import datetime


# 초기 챌린지 추천 API 스키마
# Request Models
class SurveyAnswer(BaseModel):
    """설문 응답 항목"""
    survey_number: int = Field(..., gt=0)
    tag: str = Field(...)
    answer: int = Field(..., ge=0, le=3)
    
    @field_validator('tag')
    @classmethod
    def validate_tag(cls, v: str) -> str:
        if v not in ['LIFESTYLE', 'SOCIAL', 'INNER']:
            raise ValueError(f"허용되지 않은 태그: {v}. 허용 태그: LIFESTYLE, SOCIAL, INNER")
        return v


class InitialRecommendationRequest(BaseModel):
    """초기 챌린지 추천 요청"""
    user_code: str = Field(..., min_length=1)
    recovery_level: int = Field(..., ge=1, le=3)
    survey_responses: List[SurveyAnswer] = Field(..., min_length=1)


# Response Models
class RecommendationItem(BaseModel):
    """추천 챌린지 항목"""
    challenge_id: int = Field(..., alias="challengeId")
    weight: float = Field(..., ge=0.0, le=1.0)
    
    class Config:
        populate_by_name = True


class RecommendationMetadata(BaseModel):
    """추천 메타데이터"""
    generated_at: datetime = Field(default_factory=datetime.now, alias="generatedAt")
    
    class Config:
        populate_by_name = True


class InitialRecommendationResult(BaseModel):
    """초기 추천 결과 (20개)"""
    recommendations: List[RecommendationItem]
    metadata: RecommendationMetadata = Field(default_factory=RecommendationMetadata)
    
    @field_validator('recommendations')
    @classmethod
    def validate_recommendations_count(cls, v: List[RecommendationItem]) -> List[RecommendationItem]:
        if len(v) != 20:
            raise ValueError(f"추천 개수는 정확히 20개여야 합니다. 현재: {len(v)}개")
        return v

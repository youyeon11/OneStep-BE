from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime
from enum import Enum


class InteractionType(str, Enum):
    ASSIGNED = "ASSIGNED"
    COMPLETED = "COMPLETED"
    SKIPPED = "SKIPPED"


class UserInteractionDocument(BaseModel):
    """MongoDB 상호작용 로그 문서"""
    
    user_code: str = Field(..., alias="userCode")
    challenge_id: int = Field(..., alias="challengeId")
    interaction_type: str = Field(..., alias="interactionType")
    emotion: Optional[int] = Field(None, ge=1, le=5)
    assigned_date: str = Field(..., alias="assignedDate")
    created_at: datetime = Field(default_factory=datetime.now, alias="createdAt")
    
    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "userCode": "U1001",
                "challengeId": 45,
                "interactionType": "COMPLETED",
                "emotion": 4,
                "assignedDate": "2026-02-01",
                "createdAt": "2026-02-01T13:00:00Z"
            }
        }

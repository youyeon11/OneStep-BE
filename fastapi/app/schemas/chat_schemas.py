from pydantic import BaseModel, Field
from typing import Optional, List, Literal


class ChatStartRequest(BaseModel):
    context: Optional[str] = Field(None, max_length=500)


class ChatMessageRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=1000)


class ChatStartResponse(BaseModel):
    chat_session_id: int = Field(..., alias="chatSessionId")
    
    class Config:
        populate_by_name = True


class ChatRecommendItem(BaseModel):
    challenge_code: Optional[int] = Field(None, alias="challengeCode")
    title: str
    origin: str = Field(..., alias="origin")
    status: str = Field(default="ASSIGNED", alias="status")
    reward: int = Field(default=0, alias="reward")

    class Config:
        populate_by_name = True


class ChatMessageResponse(BaseModel):
    type: Literal["common", "recommend", "segment"] = Field(..., alias="type")
    response: str
    chat_session_id: int = Field(..., alias="chatSessionId")
    recommends: Optional[List[ChatRecommendItem]] = None

    class Config:
        populate_by_name = True

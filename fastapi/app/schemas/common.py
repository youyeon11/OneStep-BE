from pydantic import BaseModel, Field, model_serializer
from typing import Optional, Generic, TypeVar
from datetime import datetime

T = TypeVar('T')


class JsonResult(BaseModel, Generic[T]):
    """Spring Backend와 동일한 API 응답 형식"""

    is_success: bool = Field(..., alias="isSuccess")
    code: int = Field(...)
    message: str = Field(...)
    result: Optional[T] = Field(None)

    class Config:
        populate_by_name = True

    @model_serializer(mode="plain")
    def _serialize_by_alias(self):
        def _serialize_value(v):
            if v is None:
                return None
            if hasattr(v, "model_dump"):
                return v.model_dump(by_alias=True, mode="json")
            if isinstance(v, list):
                return [_serialize_value(x) for x in v]
            if isinstance(v, dict):
                return {k: _serialize_value(x) for k, x in v.items()}
            return v

        return {
            "isSuccess": self.is_success,
            "code": self.code,
            "message": self.message,
            "result": _serialize_value(self.result),
        }
    
    @classmethod
    def success(cls, result: Optional[T] = None, message: str = "요청에 성공하였습니다.") -> "JsonResult[T]":
        return cls(
            is_success=True,
            code=200,
            message=message,
            result=result
        )
    
    @classmethod
    def fail(cls, code: int = 400, message: str = "요청에 실패하였습니다.") -> "JsonResult[T]":
        return cls(
            is_success=False,
            code=code,
            message=message,
            result=None
        )

class Metadata(BaseModel):
    generated_at: datetime = Field(default_factory=datetime.now, alias="generatedAt", description="생성 시각")
    
    class Config:
        populate_by_name = True

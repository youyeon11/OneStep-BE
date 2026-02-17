from fastapi import HTTPException, status, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from config.settings import settings
from app.exceptions.error_code import ErrorCode
from app.exceptions.custom_exceptions import BaseAPIException
import base64
from jose import jwt, JWTError, ExpiredSignatureError
from typing import Optional

security = HTTPBearer(
    scheme_name="Bearer Token",
    description="JWT 인증 (프론트엔드 직접 통신)"
)

security_optional = HTTPBearer(auto_error=False)


class TokenPayload(BaseModel):
    user_code: str
    role: str
    exp: int


async def verify_jwt_token(
    credentials: HTTPAuthorizationCredentials = Depends(security)
) -> TokenPayload:
    token = credentials.credentials
    
    if not token:
        raise BaseAPIException(ErrorCode.TOKEN_NOT_FOUND)
    
    try:
        secret_key_str = settings.JWT_SECRET_KEY
        missing_padding = len(secret_key_str) % 4
        if missing_padding:
            secret_key_str += '=' * (4 - missing_padding)

        decoded_key = base64.b64decode(secret_key_str)
        
        payload = jwt.decode(
            token,
            decoded_key, 
            algorithms=["HS256"]
        )
        
        user_code = payload.get("sub")
        role = payload.get("role")
        exp = payload.get("exp")
        
        if user_code is None:
            raise BaseAPIException(ErrorCode.TOKEN_INVALID)
        
        return TokenPayload(
            user_code=user_code,
            role=role or "USER",
            exp=exp or 0
        )
    
    except ExpiredSignatureError:
        raise BaseAPIException(ErrorCode.TOKEN_EXPIRED)
    except JWTError:
        raise BaseAPIException(ErrorCode.TOKEN_SIGNATURE_INVALID)


async def verify_jwt_token_optional(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_optional)
) -> Optional[TokenPayload]:
    if credentials is None:
        return None
    return await verify_jwt_token(credentials)


# Spring Backend 내부 API 인증 (Kafka 배치용, 레거시 호환)
async def verify_internal_api_key(
    credentials: HTTPAuthorizationCredentials = Depends(security)
) -> bool:
    token = credentials.credentials
    
    if not token:
        raise BaseAPIException(ErrorCode.TOKEN_NOT_FOUND)
    
    try:
        decoded_key = base64.b64decode(settings.JWT_SECRET_KEY)
        payload = jwt.decode(
            token,
            decoded_key,
            algorithms=["HS256"]
        )
        
        user_code = payload.get("sub")
        if user_code is None:
            raise BaseAPIException(ErrorCode.TOKEN_INVALID)
        
        return True
    
    except ExpiredSignatureError:
        raise BaseAPIException(ErrorCode.TOKEN_EXPIRED)
    except JWTError:
        raise BaseAPIException(ErrorCode.TOKEN_SIGNATURE_INVALID)
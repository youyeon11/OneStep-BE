from fastapi import APIRouter, Depends, Body

from app.schemas.common import JsonResult
from app.schemas.chat_schemas import (
    ChatStartRequest,
    ChatMessageRequest,
    ChatStartResponse,
    ChatMessageResponse,
    ChatRecommendItem,
)
from app.dependencies.auth import verify_jwt_token, TokenPayload
from app.dependencies.mongodb import get_mongo_db
from app.services.chat_service import ChatService
from app.exceptions.custom_exceptions import BaseAPIException
from app.exceptions.error_code import ErrorCode
from app.utils.logger import app_logger as logger

router = APIRouter(
    prefix="/api/v1/chat",
    tags=["Chat"]
)


@router.post(
    "/start",
    response_model=JsonResult[ChatStartResponse],
    summary="채팅 시작",
    response_description="생성된 chatSessionId 반환. context 있으면 펫 첫 메시지로 저장, 없으면 빈 채팅방."
)
async def start_chat(
    request: ChatStartRequest | None = Body(None),
    token: TokenPayload = Depends(verify_jwt_token),
    mongo_db = Depends(get_mongo_db)
) -> JsonResult[ChatStartResponse]:
    context = request.context if request else None
    logger.info(f"[채팅 시작] user_code={token.user_code}, context={bool(context)}")
    
    chat_service = ChatService(mongo_db)
    session_id = await chat_service.create_session(
        user_code=token.user_code,
        initial_context=context
    )
    
    return JsonResult.success(
        result=ChatStartResponse(chat_session_id=session_id)
    )


@router.post("/{chat_session_id}", response_model=JsonResult[ChatMessageResponse])
async def send_message(
    chat_session_id: int,
    request: ChatMessageRequest,
    token: TokenPayload = Depends(verify_jwt_token),
    mongo_db = Depends(get_mongo_db)
) -> JsonResult[ChatMessageResponse]:
    logger.info(f"[메시지 수신] user_code={token.user_code}, "
               f"session_id={chat_session_id}, message={request.message[:50]}...")
    
    chat_service = ChatService(mongo_db)
    
    # 세션 소유권 검증
    if not await chat_service.validate_session(chat_session_id, token.user_code):
        raise BaseAPIException(ErrorCode.CHAT_SESSION_NOT_FOUND)
    
    ai_response = await chat_service.process_message(
        session_id=chat_session_id,
        user_code=token.user_code,
        message=request.message
    )

    recommends = None
    if ai_response.get("recommends"):
        recommends = [ChatRecommendItem.model_validate(r) for r in ai_response["recommends"]]

    return JsonResult.success(
        result=ChatMessageResponse(
            type=ai_response["type"],
            response=ai_response["response"],
            chat_session_id=chat_session_id,
            recommends=recommends
        )
    )

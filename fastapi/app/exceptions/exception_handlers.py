from fastapi import Request, status
from fastapi.responses import JSONResponse
from app.exceptions.custom_exceptions import BaseAPIException
from loguru import logger

async def api_exception_handler(request: Request, exc: BaseAPIException):
    """API 예외 핸들러"""
    logger.error(f"API exception: [{exc.error_code.error_code}] {exc.detail} - Path: {request.url.path}")
    
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "isSuccess": False,
            "code": exc.error_code.error_code,
            "message": exc.detail,
            "result": None
        }
    )

async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unexpected exception: {str(exc)} - Path: {request.url.path}", exc_info=True)
    
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "isSuccess": False,
            "code": "SERVER001",
            "message": "서버 내부 오류가 발생했습니다.",
            "result": None
        }
    )

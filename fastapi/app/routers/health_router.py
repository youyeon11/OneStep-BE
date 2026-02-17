from fastapi import APIRouter
from datetime import datetime

router = APIRouter(tags=["Health"])

@router.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "service": "FastAPI AI Server",
        "version": "0.1.0"
    }

@router.get("/health/ready")
async def readiness_check():
    return {
        "status": "ready",
        "checks": {
            "mongodb": "ok",
            "kafka": "ok"
        },
        "timestamp": datetime.now().isoformat()
    }

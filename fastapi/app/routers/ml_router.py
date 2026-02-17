from fastapi import APIRouter, Depends, HTTPException
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.dependencies.mongodb import get_mongo_db
from app.ml.scheduler import ml_scheduler
from app.ml.model_manager import model_manager
from app.repositories.interaction_repository import InteractionRepository
from app.recommenders.factory import ModelType
from app.utils.logger import app_logger as logger

router = APIRouter(prefix="/api/v1/ml", tags=["ML Models"])


@router.get("/status")
async def get_ml_status(mongo_db: AsyncIOMotorDatabase = Depends(get_mongo_db)):
    """ML 시스템 상태 조회"""
    interaction_repo = InteractionRepository(mongo_db)
    stats = await interaction_repo.get_stats()
    
    return {
        "training_manager": ml_scheduler.get_status(),
        "available_models": model_manager.get_available_models(),
        "interaction_stats": stats,
        "model_metadata": {
            model_type.value: model_manager.get_metadata(model_type)
            for model_type in [ModelType.LIGHTFM, ModelType.ALS, ModelType.TWO_STAGE]
            if model_manager.get_metadata(model_type)
        }
    }


@router.post("/train")
async def trigger_training(mongo_db: AsyncIOMotorDatabase = Depends(get_mongo_db)):
    """수동 모델 학습 트리거"""
    logger.info("[API] 수동 모델 학습 요청")
    
    ml_scheduler.configure(mongo_db)
    results = await ml_scheduler.force_train()
    
    return {
        "message": "모델 학습 완료",
        "results": results
    }


@router.get("/interactions/stats")
async def get_interaction_stats(mongo_db: AsyncIOMotorDatabase = Depends(get_mongo_db)):
    """상호작용 통계 조회"""
    interaction_repo = InteractionRepository(mongo_db)
    stats = await interaction_repo.get_stats()
    
    return stats


@router.get("/models/{model_type}")
async def get_model_info(model_type: str):
    """특정 모델 정보 조회"""
    from app.recommenders.factory import ModelType
    
    try:
        mt = ModelType(model_type)
    except ValueError:
        raise HTTPException(status_code=400, detail=f"Invalid model type: {model_type}")
    
    exists = model_manager.exists(mt)
    metadata = model_manager.get_metadata(mt)
    
    return {
        "model_type": model_type,
        "exists": exists,
        "metadata": metadata
    }

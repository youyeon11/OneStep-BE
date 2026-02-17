import asyncio
from datetime import datetime
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from config.settings import settings
from app.dependencies.mongodb import (
    connect_to_mongo, 
    close_mongo_connection,
    get_mongo_db
)
from app.kafka.handlers import start_kafka_consumer
from app.repositories.daily_recommendation_repository import DailyRecommendationRepository
from app.repositories.user_profile_repository import UserProfileRepository
from app.repositories.interaction_repository import InteractionRepository
from app.ml.scheduler import ml_scheduler
from app.kafka.producer import KafkaProducerManager
from app.routers.health_router import router as health_router
from app.routers.recommendation_router import router as recommendation_router
from app.routers.chat_router import router as chat_router
from app.routers.ml_router import router as ml_router
from app.exceptions.exception_handlers import (
    api_exception_handler,
    global_exception_handler
)
from app.exceptions.custom_exceptions import BaseAPIException
from app.utils.logger import app_logger as logger
from prometheus_fastapi_instrumentator import Instrumentator


kafka_producer = KafkaProducerManager(settings.KAFKA_BOOTSTRAP_SERVERS)

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("=" * 60)
    logger.info("FastAPI AI Server 시작 중...")
    logger.info("=" * 60)
    
    consumer_task = None
    
    try:
        logger.info("Kafka Producer 시작...")
        await kafka_producer.start()
        logger.info("Kafka Producer 시작 완료")
        
        logger.info("MongoDB 연결...")
        await connect_to_mongo()
        mongo_db = get_mongo_db()
        
        logger.info("MongoDB 인덱스 생성...")
        daily_repo = DailyRecommendationRepository(mongo_db)
        user_profile_repo = UserProfileRepository(mongo_db)
        interaction_repo = InteractionRepository(mongo_db)
        await daily_repo.create_indexes()
        await user_profile_repo.create_indexes()
        await interaction_repo.create_indexes()
        logger.info("MongoDB 인덱스 생성 완료")
        
        logger.info("Kafka Consumer 시작...")
        consumer_task = asyncio.create_task(start_kafka_consumer(mongo_db))
        logger.info("Kafka Consumer 시작 완료")
        
        logger.info("ML 스케줄러 시작...")
        ml_scheduler.configure(mongo_db)
        ml_scheduler.start()
        logger.info(f"ML 스케줄러 시작 완료 - 다음 학습: {ml_scheduler.get_next_run_time()}")
        
        logger.info("=" * 60)
        logger.info("FastAPI AI Server 시작 완료!")
        logger.info(f"환경: {settings.APP_ENV}")
        logger.info(f"디버그 모드: {settings.DEBUG}")
        logger.info("=" * 60)
        
    except Exception as e:
        logger.error(f"서버 시작 실패: {e}")
        raise
    
    yield
    
    logger.info("=" * 60)
    logger.info("FastAPI AI Server 종료 중...")
    logger.info("=" * 60)
    
    logger.info("ML 스케줄러 종료...")
    ml_scheduler.stop()
    
    logger.info("Kafka Producer 종료...")
    await kafka_producer.stop()
    
    if consumer_task:
        logger.info("Kafka Consumer 종료...")
        consumer_task.cancel()
        try:
            await consumer_task
        except asyncio.CancelledError:
            pass
    
    await close_mongo_connection()
    
    logger.info("=" * 60)
    logger.info("FastAPI AI Server 종료 완료")
    logger.info("=" * 60)

app = FastAPI(
    title="OneStep FastAPI AI Server",
    description="챌린지 추천 및 분석 AI 서버",
    version="0.1.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS.split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

Instrumentator().instrument(app).expose(app)

app.add_exception_handler(BaseAPIException, api_exception_handler)
app.add_exception_handler(Exception, global_exception_handler)

app.include_router(health_router)
app.include_router(recommendation_router)
app.include_router(chat_router)
app.include_router(ml_router)

@app.get("/")
async def root():
    return {
        "service": "OneStep FastAPI AI Server",
        "version": "0.5.0",
        "phase": "PhaseC - 채팅 API 스켈레톤",
        "status": "running",
        "docs": "/docs"
    }
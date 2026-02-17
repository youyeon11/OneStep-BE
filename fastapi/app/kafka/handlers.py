import json
from aiokafka import AIOKafkaConsumer
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.schemas.daily_recommendation_schemas import DailyRecommendationBatchMessage
from app.services.daily_recommendation_service import DailyRecommendationService
from app.recommenders.factory import ModelType
from app.repositories.interaction_repository import InteractionRepository
from app.repositories.user_profile_repository import UserProfileRepository
from app.schemas.user_profile_schemas import UserProfileDocument
from app.ml.model_manager import model_manager
from app.utils.logger import app_logger as logger
from config.settings import settings
from datetime import datetime


TOPICS = {
    "USER_ROUTINE_GENERATE": "user.routine.generate",
    "INITIAL_USER_INFO": "initial.user.topic",
}
GROUP_ID = "fastapi-recommendation-group"


async def _select_model_for_user(mongo_db: AsyncIOMotorDatabase, user_code: str) -> ModelType:
    """상호작용 수에 따라 최적 모델 자동 선택"""
    try:
        interaction_repo = InteractionRepository(mongo_db)
        
        total_interactions = await interaction_repo.count_all()
        user_interactions = await interaction_repo.count_by_user(user_code)
        
        model_type = await model_manager.select_model_type(total_interactions, user_interactions)
        
        logger.debug(f"[모델 선택] user={user_code}, total={total_interactions}, "
                    f"user_count={user_interactions}, selected={model_type.value}")
        
        return model_type
        
    except Exception as e:
        logger.warning(f"[모델 선택 실패] 기본 Rule-based 사용: {e}")
        return ModelType.RULE_BASED


async def handle_spring_kafka_message(
    message_value,
    mongo_db: AsyncIOMotorDatabase
):
    try:
        if isinstance(message_value, bytes):
            message_value = message_value.decode("utf-8")
        json_message = json.loads(message_value)

        message_type = json_message["type"]
        payload = json_message["payload"]
        message_id = json_message["id"]
        
        logger.info(f"[Spring 메시지 수신] type={message_type}, id={message_id}")
        
        if message_type == "INITIAL_SET":
            user_profile_repo = UserProfileRepository(mongo_db)
            doc = UserProfileDocument(
                user_code=payload["userCode"],
                recovery_level=payload["recoveryLevel"],
                batch_id="initial",
                pet_nickname=payload.get("petNickname"),
            )
            await user_profile_repo.upsert(doc)
            logger.info(f"[초기 유저 프로필 저장] user_code={payload['userCode']}, "
                       f"petNickname={payload.get('petNickname')}")
            return {"status": "completed", "message_id": message_id}
        
        if message_type == "ROUTINE_GENERATE_REQUEST":
            from app.schemas.daily_recommendation_schemas import UserRecommendationData
            from datetime import datetime

            user_data = UserRecommendationData(**payload)
            
            batch_message = DailyRecommendationBatchMessage(
                batchId=message_id,
                triggeredAt=datetime.now(),
                users=[user_data]
            )
            
            # 자동 모델 선택
            model_type = await _select_model_for_user(mongo_db, user_data.user_code)
            
            service = DailyRecommendationService(mongo_db, model_type=model_type)
            result = await service.process_batch(batch_message)
            
            logger.info(f"[단일 사용자 처리 완료] user_code={user_data.user_code}, "
                       f"model={model_type.value}, success={result['success_count']}")
            
            return {"status": "completed", "message_id": message_id, **result}
        else:
            logger.warning(f"[알 수 없는 메시지 타입] type={message_type}")
            return {"status": "skipped", "type": message_type}
        
    except Exception as e:
        logger.error(f"[Spring 메시지 처리 실패] error={e}", exc_info=True)
        raise

async def _safe_handle_message(message_value, mongo_db: AsyncIOMotorDatabase) -> None:
    """한 건 실패해도 컨슈머는 유지하고 다음 메시지 계속 처리"""
    try:
        await handle_spring_kafka_message(message_value, mongo_db)
    except Exception as e:
        logger.error(f"[Kafka 메시지 스킵] 처리 실패로 다음 메시지로 진행: {e}", exc_info=True)


async def start_kafka_consumer(mongo_db: AsyncIOMotorDatabase):
    consumer = AIOKafkaConsumer(
        TOPICS["USER_ROUTINE_GENERATE"],
        TOPICS["INITIAL_USER_INFO"],
        bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
        group_id=GROUP_ID,
        auto_offset_reset="earliest",
        enable_auto_commit=True
    )
    
    await consumer.start()
    logger.info(f"[Kafka Consumer 시작] topics={list(TOPICS.values())}, group_id={GROUP_ID}")
    
    try:
        async for message in consumer:
            logger.debug(f"[Kafka 메시지 수신] topic={message.topic}, "
                        f"partition={message.partition}, offset={message.offset}")
            await _safe_handle_message(message.value, mongo_db)
    finally:
        await consumer.stop()
        logger.info("[Kafka Consumer 종료]")

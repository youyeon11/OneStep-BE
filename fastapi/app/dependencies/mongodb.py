from motor.motor_asyncio import AsyncIOMotorClient
from config.settings import settings
from app.utils.constants import Collections
from loguru import logger

class MongoDB:
    client: AsyncIOMotorClient = None
    db = None

mongodb = MongoDB()

async def connect_to_mongo():
    try:
        logger.info("MongoDB 연결 시작...")
        mongodb.client = AsyncIOMotorClient(settings.MONGO_URI)
        mongodb.db = mongodb.client[settings.MONGO_DB_NAME]
        
        await mongodb.client.admin.command('ping')
        logger.info(f"MongoDB 연결 완료 (DB: {settings.MONGO_DB_NAME})")
    except Exception as e:
        logger.error(f"MongoDB 연결 실패: {e}")
        raise

async def close_mongo_connection():
    logger.info("MongoDB 연결 종료...")
    if mongodb.client:
        mongodb.client.close()
    logger.info("MongoDB 연결 종료 완료")

def get_mongo_db():
    if mongodb.db is None:
        raise Exception("MongoDB not connected")
    return mongodb.db

def get_collection(collection_name: str):
    if mongodb.db is None:
        raise Exception("MongoDB not connected")
    return mongodb.db[collection_name]

# 일일 루틴 스냅샷 저장
async def insert_custom_routine(routine_data: dict):
    try:
        if hasattr(routine_data, 'model_dump'):
            doc = routine_data.model_dump(by_alias=True)
        else:
            doc = routine_data

        collection = get_collection(Collections.DAILY_ROUTINE_SNAPSHOTS)
        result = await collection.insert_one(doc)

        logger.info(f"Daily routine snapshot inserted: {result.inserted_id}")
        return str(result.inserted_id)
    except Exception as e:
        logger.error(f"Failed to insert routine: {e}")
        raise

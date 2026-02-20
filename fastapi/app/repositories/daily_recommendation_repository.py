from typing import Optional
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.schemas.daily_recommendation_schemas import DailyRecommendationDocument
from app.utils.logger import app_logger as logger


# MongoDB에서 일일 추천 결과 CRUD
class DailyRecommendationRepository:
    
    COLLECTION_NAME = "daily_routine_snapshots"
    
    def __init__(self, mongo_db: AsyncIOMotorDatabase):
        self.collection = mongo_db[self.COLLECTION_NAME]
    
    async def upsert(self, document: DailyRecommendationDocument) -> None:
        await self.collection.update_one(
            {"userCode": document.user_code},
            {"$set": document.model_dump(by_alias=True)},
            upsert=True
        )
        logger.debug(f"[MongoDB 저장] userCode={document.user_code}")
    
    async def find_by_user_code(self, user_code: str) -> Optional[dict]:
        document = await self.collection.find_one(
            {"userCode": user_code},
            sort=[("createdAt", -1)]
        )
        return document
    
    async def delete_by_user_code(self, user_code: str) -> int:
        result = await self.collection.delete_many({"userCode": user_code})
        logger.debug(f"[MongoDB 삭제] userCode={user_code}, "
                    f"deleted_count={result.deleted_count}")
        return result.deleted_count
    
    async def create_indexes(self) -> None:
        await self.collection.create_index("userCode", unique=True)
        await self.collection.create_index("createdAt")
        logger.info("[MongoDB 인덱스 생성 완료] daily_routine_snapshots")

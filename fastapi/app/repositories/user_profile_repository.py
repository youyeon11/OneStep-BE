from typing import List, Optional
from datetime import datetime
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.schemas.user_profile_schemas import UserProfileDocument
from app.utils.logger import app_logger as logger


# MongoDB에서 유저 프로필 CRUD
class UserProfileRepository:
    
    COLLECTION_NAME = "user_profiles"
    
    def __init__(self, mongo_db: AsyncIOMotorDatabase):
        self.collection = mongo_db[self.COLLECTION_NAME]
    
    # 유저 프로필 저장
    async def upsert(self, document: UserProfileDocument) -> None:
        await self.collection.update_one(
            {"user_code": document.user_code},
            {"$set": document.model_dump(by_alias=True, exclude_none=True)},
            upsert=True
        )
        logger.debug(f"[유저 프로필 저장] user_code={document.user_code}")
    
    # 유저 프로필 조회
    async def find_by_user_code(self, user_code: str) -> Optional[dict]:
        return await self.collection.find_one({"user_code": user_code})

    def _minimal_profile_on_insert(self, user_code: str, pet_nickname: Optional[str] = None) -> dict:
        doc = {
            "user_code": user_code,
            "recoveryLevel": 2,
            "batchId": "chat",
            "completedChallengeCount": 0,
            "recentChallengeIds": [],
            "surveyTags": [],
            "preferredCategories": [],
            "interestTags": [],
        }
        if pet_nickname is not None:
            doc["petNickname"] = pet_nickname
        return doc

    async def update_interest_tags(self, user_code: str, tags: List[str]) -> bool:
        result = await self.collection.update_one(
            {"user_code": user_code},
            {
                "$set": {"interestTags": tags, "updatedAt": datetime.now()},
                "$setOnInsert": self._minimal_profile_on_insert(user_code),
            },
            upsert=True,
        )
        logger.debug(f"[유저 관심사 업데이트] user_code={user_code}, tags={tags}")
        return True

    async def update_from_chat(
        self,
        user_code: str,
        *,
        interest_tags: Optional[List[str]] = None,
        recent_mood: Optional[str] = None,
        chat_analysis: Optional[str] = None
    ) -> None:
        updates = {"updatedAt": datetime.now()}
        if interest_tags is not None:
            updates["interestTags"] = interest_tags
        if recent_mood is not None:
            updates["recentMood"] = recent_mood
        if chat_analysis is not None:
            updates["chatAnalysis"] = chat_analysis
        set_on_insert = self._minimal_profile_on_insert(user_code)
        for key in list(set_on_insert.keys()):
            if key in updates:
                del set_on_insert[key]
        await self.collection.update_one(
            {"user_code": user_code},
            {"$set": updates, "$setOnInsert": set_on_insert},
            upsert=True,
        )
        logger.debug(f"[user_profiles 채팅 반영] user_code={user_code}")

    async def create_indexes(self) -> None:
        await self.collection.create_index("user_code", unique=True)
        logger.info("[MongoDB] user_profiles 인덱스 생성 완료")

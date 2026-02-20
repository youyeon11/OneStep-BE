from typing import List, Optional, Dict
from motor.motor_asyncio import AsyncIOMotorDatabase
from datetime import datetime, timedelta

from app.schemas.interaction_schemas import UserInteractionDocument
from app.utils.logger import app_logger as logger


class InteractionRepository:
    """MongoDB 상호작용 로그 CRUD"""
    
    COLLECTION_NAME = "user_interactions"
    
    def __init__(self, mongo_db: AsyncIOMotorDatabase):
        self.collection = mongo_db[self.COLLECTION_NAME]
    
    async def upsert(self, document: UserInteractionDocument) -> None:
        """상호작용 저장 (중복 방지: user_code + challenge_id + assigned_date)"""
        await self.collection.update_one(
            {
                "userCode": document.user_code,
                "challengeId": document.challenge_id,
                "assignedDate": document.assigned_date
            },
            {"$set": document.model_dump(by_alias=True)},
            upsert=True
        )
    
    async def bulk_upsert(self, documents: List[UserInteractionDocument]) -> int:
        """배치 상호작용 저장"""
        if not documents:
            return 0
        
        count = 0
        for doc in documents:
            await self.upsert(doc)
            count += 1
        
        return count
    
    async def count_all(self) -> int:
        """전체 상호작용 수"""
        return await self.collection.count_documents({})
    
    async def count_by_user(self, user_code: str) -> int:
        """유저별 상호작용 수"""
        return await self.collection.count_documents({"userCode": user_code})
    
    async def count_completed(self) -> int:
        """완료된 상호작용 수 (학습 데이터)"""
        return await self.collection.count_documents({"interactionType": "COMPLETED"})
    
    async def get_all_for_training(self, limit: int = 100000) -> List[Dict]:
        """학습용 상호작용 데이터 조회"""
        cursor = self.collection.find(
            {"interactionType": {"$in": ["COMPLETED", "ASSIGNED"]}},
            {"_id": 0, "userCode": 1, "challengeId": 1, "interactionType": 1, "emotion": 1}
        ).limit(limit)
        
        return await cursor.to_list(length=limit)
    
    async def get_user_interactions(self, user_code: str) -> List[Dict]:
        """유저별 상호작용 조회"""
        cursor = self.collection.find(
            {"userCode": user_code},
            {"_id": 0}
        ).sort("createdAt", -1)
        
        return await cursor.to_list(length=100)
    
    async def get_stats(self) -> Dict:
        """상호작용 통계"""
        total = await self.count_all()
        completed = await self.count_completed()
        
        # 유니크 유저 수
        unique_users = await self.collection.distinct("userCode")
        
        # 유니크 챌린지 수
        unique_challenges = await self.collection.distinct("challengeId")
        
        return {
            "total_interactions": total,
            "completed_interactions": completed,
            "unique_users": len(unique_users),
            "unique_challenges": len(unique_challenges),
            "avg_interactions_per_user": round(total / len(unique_users), 2) if unique_users else 0
        }
    
    async def create_indexes(self) -> None:
        """인덱스 생성"""
        await self.collection.create_index(
            [("userCode", 1), ("challengeId", 1), ("assignedDate", 1)],
            unique=True
        )
        await self.collection.create_index("userCode")
        await self.collection.create_index("interactionType")
        await self.collection.create_index("createdAt")
        logger.info("[MongoDB] user_interactions 인덱스 생성 완료")

from typing import List, Dict, Optional
from motor.motor_asyncio import AsyncIOMotorDatabase
from app.utils.logger import app_logger as logger


# MongoDB에서 챌린지 데이터 조회
class ChallengeRepository:
    
    def __init__(self, mongo_db: AsyncIOMotorDatabase):
        self.collection = mongo_db["challenges"]
    
    # 키워드(제목/태그) + 난이도로 챌린지 조회 (추천 매칭용)
    async def find_by_keywords(
        self,
        keywords: List[str],
        max_difficulty_level: int,
        limit: int = 5,
    ) -> List[Dict]:
        if not keywords:
            return []
        or_conditions = []
        for k in keywords:
            k = (k or "").strip()
            if not k:
                continue
            or_conditions.append({"title": {"$regex": k, "$options": "i"}})
            or_conditions.append({"tags": k})
        if not or_conditions:
            return []
        query = {
            "difficulty_level": {"$lte": max_difficulty_level},
            "$or": or_conditions,
        }
        cursor = self.collection.find(query).limit(limit)
        challenges = await cursor.to_list(length=limit)
        logger.info(f"[MongoDB] 키워드 챌린지 조회: keywords={keywords}, count={len(challenges)}")
        return challenges

    # 난이도 필터링으로 챌린지 조회
    async def find_by_difficulty(
        self,
        max_difficulty_level: int
    ) -> List[Dict]:

        cursor = self.collection.find({
            "difficulty_level": {"$lte": max_difficulty_level}
        })
        
        challenges = await cursor.to_list(length=None)
        logger.info(f"MongoDB 챌린지 조회 완료 - max_difficulty: {max_difficulty_level}, count: {len(challenges)}")
        
        return challenges
    
    # 카테고리와 난이도로 챌린지 조회
    async def find_by_category_and_difficulty(
        self,
        category: str,
        max_difficulty_level: int,
        limit: Optional[int] = None
    ) -> List[Dict]:

        query = {
            "category": category,
            "difficulty_level": {"$lte": max_difficulty_level}
        }
        
        cursor = self.collection.find(query)
        
        if limit:
            cursor = cursor.limit(limit)
        
        challenges = await cursor.to_list(length=None)
        logger.debug(f"카테고리 {category} 챌린지 조회: {len(challenges)}개")
        
        return challenges
    
    # 카테고리별 챌린지 개수 조회
    async def count_by_category(
        self,
        category: str,
        max_difficulty_level: int
    ) -> int:

        count = await self.collection.count_documents({
            "category": category,
            "difficulty_level": {"$lte": max_difficulty_level}
        })
        return count
    
    # 챌린지 ID 목록으로 조회
    async def find_by_ids(
        self,
        challenge_ids: List[int]
    ) -> List[Dict]:

        cursor = self.collection.find({
            "challenge_code": {"$in": challenge_ids}
        })
        
        challenges = await cursor.to_list(length=None)
        return challenges

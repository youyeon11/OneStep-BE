import asyncio
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from motor.motor_asyncio import AsyncIOMotorClient
from app.services.vector_store import VectorStoreService
from config.settings import settings
from app.utils.logger import app_logger as logger

# MongoDB에서 챌린지 데이터를 ChromaDB에 삽입
async def main():
    logger.info("=" * 60)
    logger.info("ChromaDB 초기화 시작")
    logger.info("=" * 60)
    
    # MongoDB 연결
    mongo_client = AsyncIOMotorClient(
        f"mongodb://{settings.MONGO_USERNAME}:{settings.MONGO_PASSWORD}"
        f"@{settings.MONGO_HOST}:{settings.MONGO_PORT}/?authSource=admin"
    )
    mongo_db = mongo_client[settings.MONGO_DB_NAME]
    logger.info(f"MongoDB 연결 완료: {settings.MONGO_HOST}:{settings.MONGO_PORT}/{settings.MONGO_DB_NAME}")
    
    try:
        challenges_collection = mongo_db["challenges"]
        challenges_cursor = challenges_collection.find({})
        challenges = await challenges_cursor.to_list(length=None)
        
        logger.info(f"MongoDB에서 {len(challenges)}개의 챌린지 조회 완료")
        
        if not challenges:
            logger.warning("MongoDB에 챌린지 데이터가 없습니다!")
            return
        
        formatted_challenges = []
        for challenge in challenges:
            # ID 필드 확인 (challenge_code 또는 challenge_master_id)
            challenge_id = challenge.get("challenge_code") or challenge.get("challenge_master_id")
            if not challenge_id:
                logger.warning(f"챌린지 ID 없음, 스킵: {challenge.get('title', 'Unknown')}")
                continue
            
            formatted = {
                "challengeId": challenge_id,
                "title": challenge.get("title", ""),
                "description": challenge.get("description", ""), 
                "category": challenge.get("category", ""),
                "difficultyLevel": challenge.get("difficulty_level") or challenge.get("difficultyLevel", 1),
                "tags": challenge.get("tags", [])
            }
            formatted_challenges.append(formatted)
            
            # 디버깅용 로그 (처음 3개만)
            if len(formatted_challenges) <= 3:
                logger.info(
                    f"  [미리보기] ID:{formatted['challengeId']} | {formatted['title']} "
                    f"({formatted['category']})"
                )
        
        logger.info("ChromaDB에 임베딩 시작...")
        
        # 싱글톤 인스턴스 호출
        vector_store = VectorStoreService()
        
        # 데이터 삽입 (내부에서 개선된 텍스트 포맷팅 로직 수행됨)
        await vector_store.add_challenges(formatted_challenges)
        
        logger.info("=" * 60)
        logger.info(f"✅ ChromaDB 초기화 완료! {len(formatted_challenges)}개 챌린지 임베딩됨")
        logger.info("=" * 60)
        
    except Exception as e:
        logger.error(f"❌ 오류 발생: {e}", exc_info=True)
        raise
    finally:
        mongo_client.close()
        logger.info("MongoDB 연결 종료")

if __name__ == "__main__":
    asyncio.run(main())
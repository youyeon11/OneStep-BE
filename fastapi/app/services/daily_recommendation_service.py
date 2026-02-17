from typing import Dict, List, Optional
from collections import Counter
from datetime import datetime, timedelta
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.schemas.daily_recommendation_schemas import (
    DailyRecommendationBatchMessage,
    UserRecommendationData,
    DailyRecommendationDocument,
    RecommendationItem,
    RecommendationMetadata,
    CreatedAtInfo
)
from app.schemas.user_profile_schemas import UserProfileDocument
from app.schemas.interaction_schemas import UserInteractionDocument
from app.recommenders.factory import RecommenderFactory, ModelType
from app.recommenders.base import BaseRecommender
from app.repositories.challenge_repository import ChallengeRepository
from app.repositories.daily_recommendation_repository import DailyRecommendationRepository
from app.repositories.user_profile_repository import UserProfileRepository
from app.repositories.interaction_repository import InteractionRepository
from app.ml.scheduler import ml_scheduler
from app.utils.logger import app_logger as logger


# 일일 챌린지 추천 서비스
class DailyRecommendationService:
    DEFAULT_COUNT = 5
    
    def __init__(
        self, 
        mongo_db: AsyncIOMotorDatabase,
        model_type: ModelType = ModelType.RULE_BASED
    ):
        self.mongo_db = mongo_db
        self.recommender: BaseRecommender = RecommenderFactory.get_recommender(model_type)
        self.challenge_repo = ChallengeRepository(mongo_db)
        self.recommendation_repo = DailyRecommendationRepository(mongo_db)
        self.user_profile_repo = UserProfileRepository(mongo_db)
        self.interaction_repo = InteractionRepository(mongo_db)
    
    # 배치 메시지 처리
    async def process_batch(self, message: DailyRecommendationBatchMessage) -> Dict[str, int]:
        logger.info(f"[배치 시작] batch_id={message.batch_id}, user_count={len(message.users)}")
        
        success_count, fail_count = 0, 0
        
        for user_data in message.users:
            try:
                await self._process_single_user(user_data, message.batch_id)
                success_count += 1
            except Exception as e:
                logger.error(f"[유저 추천 실패] user_code={user_data.user_code}, error={e}")
                fail_count += 1
        
        logger.info(f"[배치 완료] batch_id={message.batch_id}, success={success_count}, fail={fail_count}")
        
        # 배치 완료 후 ML 모델 학습 조건 확인
        await ml_scheduler.check_and_train()
        
        return {"success_count": success_count, "fail_count": fail_count}

    # 단일 유저 추천 생성 및 MongoDB 저장
    async def _process_single_user(self, user_data: UserRecommendationData, batch_id: str) -> None:
        if user_data.user_context is None:
            raise ValueError(f"user_context가 없습니다. user_code={user_data.user_code}")
        
        history_filtered = self._filter_challenge_history(user_data.challenge_history or [])
        route = self._filter_route(user_data.route)
        existing_profile = await self.user_profile_repo.find_by_user_code(user_data.user_code)
        interest_tags = (existing_profile or {}).get("interestTags") or []
        preferred_categories = (existing_profile or {}).get("preferredCategories") or []
        preferred_categories = await self._derive_preferred_categories(
            user_data.challenge_history or [], preferred_categories
        )
        
        all_challenges = await self.challenge_repo.find_by_difficulty(
            max_difficulty_level=user_data.user_context.recovery_level
        )
        excluded_ids = await self._get_excluded_ids(
            user_data.user_code,
            user_data.challenge_history or [],
            user_data.request_date
        )
        def _code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        item_pool = [c for c in all_challenges if _code(c) not in excluded_ids]
        
        if not item_pool:
            raise ValueError("추천 가능한 챌린지가 없습니다.")
        results = await self.recommender.recommend(
            user_features={
                "user_code": user_data.user_code,
                "interest_tags": interest_tags,
                "preferred_categories": preferred_categories,
                "recovery_level": user_data.user_context.recovery_level
            },
            item_pool=item_pool,
            history=[h.model_dump() for h in history_filtered],
            top_k=self.DEFAULT_COUNT
        )
        
        recommendations = [
            RecommendationItem(challengeCode=r.challenge_id, weight=r.weight)
            for r in results
        ]
        
        document = DailyRecommendationDocument(
            userCode=user_data.user_code,
            recommendations=recommendations,
            route=route,
            metadata=RecommendationMetadata(generatedAt=datetime.now()),
            createdAt=CreatedAtInfo(date=datetime.now())
        )
        
        await self.recommendation_repo.upsert(document)
        
        # 유저 프로필 MongoDB 저장 (챗봇 개인화용)
        user_profile = self._build_user_profile(
            user_data, batch_id, interest_tags, preferred_categories
        )
        await self.user_profile_repo.upsert(user_profile)
        
        # 상호작용 로그 저장 (ML 모델 학습용)
        await self._save_interactions(user_data)
        
        logger.debug(f"[유저 추천 완료] user_code={user_data.user_code}, "
                    f"model={self.recommender.get_model_info()['model_type']}, "
                    f"count={len(recommendations)}")
    
    async def _derive_preferred_categories(
        self, history: list, fallback: List[str]
    ) -> List[str]:
        completed_ids = [
            h.challenge_master_id for h in history
            if h.challenge_status == "COMPLETED" and h.challenge_master_id is not None
        ]
        if not completed_ids:
            return fallback
        challenges = await self.challenge_repo.find_by_ids(completed_ids)
        categories = [c.get("category") for c in challenges if c.get("category")]
        if not categories:
            return fallback
        top2 = [cat for cat, _ in Counter(categories).most_common(2)]
        return top2 or fallback

    def _filter_challenge_history(
        self, history: list
    ) -> list:
        IGNORE_STATUS = {"PROGRESS", "CANCELED"}
        IGNORE_ORIGIN = {"SELF", "LETTER"}
        return [
            h for h in history
            if (h.challenge_status or "") not in IGNORE_STATUS
            and (h.origin or "") not in IGNORE_ORIGIN
        ]

    def _filter_route(self, route) -> Optional[any]:
        if route is None:
            return None
        if getattr(route, "challenge_status", None) in ("PROGRESS", "CANCELED"):
            return None
        return route

    def _build_user_profile(
        self,
        user_data: UserRecommendationData,
        batch_id: str,
        existing_interest_tags: list,
        existing_preferred_categories: list
    ) -> UserProfileDocument:
        if user_data.user_context is None:
            raise ValueError(f"user_context가 없어 유저 프로필을 생성할 수 없습니다. user_code={user_data.user_code}")
        
        history = user_data.challenge_history or []
        completed = [h for h in history if h.challenge_status == "COMPLETED"]
        completed_count = len(completed)
        
        emotions = [h.emotion for h in completed if h.emotion is not None]
        avg_emotion = sum(emotions) / len(emotions) if emotions else None
        
        recent_ids = [
            h.challenge_master_id
            for h in completed[:5]
            if h.challenge_master_id is not None
        ]
        
        return UserProfileDocument(
            userCode=user_data.user_code,
            recoveryLevel=user_data.user_context.recovery_level,
            surveyTags=[],
            interestTags=existing_interest_tags,
            completedChallengeCount=completed_count,
            avgEmotionScore=avg_emotion,
            preferredCategories=existing_preferred_categories[:2],
            recentChallengeIds=recent_ids,
            updatedAt=datetime.now(),
            batchId=batch_id
        )
    
    async def _save_interactions(self, user_data: UserRecommendationData) -> None:
        """challenge_history를 상호작용 로그로 저장 (ML 모델 학습용)"""
        if not user_data.challenge_history:
            return
        
        interactions = []
        for h in user_data.challenge_history:
            master_id = h.challenge_master_id if h.challenge_master_id is not None else h.challenge_id
            if master_id is None or h.assigned_date is None:
                continue
            
            interaction = UserInteractionDocument(
                userCode=user_data.user_code,
                challengeId=master_id,
                interactionType=h.challenge_status or "ASSIGNED",
                emotion=h.emotion,
                assignedDate=h.assigned_date
            )
            interactions.append(interaction)
        
        if interactions:
            saved = await self.interaction_repo.bulk_upsert(interactions)
            logger.debug(f"[상호작용 저장] user_code={user_data.user_code}, count={saved}")
    
    # 제외 로직: MongoDB 최근 추천 + 전날 할당 챌린지 제외
    async def _get_excluded_ids(
        self, 
        user_code: str,
        history: list, 
        request_date: str
    ) -> set:
        excluded = set()
        
        # 1. 전날 할당된 챌린지 제외 (이틀 연속 방지, 챌린지 테이블 ID 기준)
        req_date = datetime.strptime(request_date, "%Y-%m-%d").date()
        yesterday = (req_date - timedelta(days=1)).strftime("%Y-%m-%d")
        for h in history:
            if h.assigned_date != yesterday:
                continue
            mid = h.challenge_master_id if h.challenge_master_id is not None else h.challenge_id
            if mid is not None:
                excluded.add(mid)
        
        # 2. MongoDB에서 최근 추천 가져오기 (빠른 중복 방지)
        recent_recommendations = await self.recommendation_repo.find_by_user_code(user_code)
        if recent_recommendations:
            # 당일 추천이 이미 있다면 해당 추천 제외
            created_date = recent_recommendations.get("createdAt", {}).get("date")
            if created_date:
                created_date_str = created_date.strftime("%Y-%m-%d") if hasattr(created_date, 'strftime') else str(created_date)[:10]
                if created_date_str == request_date:
                    for r in recent_recommendations.get("recommendations", []):
                        mid = r.get("challengeCode") or r.get("challengeMasterId") or r.get("challengeId")
                        if mid is not None:
                            excluded.add(mid)
                    logger.debug(f"[중복 방지] 당일 추천 제외: {len(excluded)}개")
        
        return excluded

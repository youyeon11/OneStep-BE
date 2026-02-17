from typing import Dict, List, Optional
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.repositories.interaction_repository import InteractionRepository
from app.repositories.challenge_repository import ChallengeRepository
from app.recommenders.factory import ModelType
from app.recommenders.lightfm_recommender import LightFMRecommender
from app.recommenders.als_recommender import ALSRecommender
from app.recommenders.two_stage_recommender import TwoStageRecommender
from app.ml.model_manager import model_manager
from app.utils.logger import app_logger as logger


# 모델 학습 관리
class ModelTrainer:
    
    MIN_INTERACTIONS_LIGHTFM = 500
    MIN_INTERACTIONS_ALS = 2000
    MIN_INTERACTIONS_TWO_STAGE = 5000
    
    def __init__(self, mongo_db: AsyncIOMotorDatabase):
        self.mongo_db = mongo_db
        self.interaction_repo = InteractionRepository(mongo_db)
        self.challenge_repo = ChallengeRepository(mongo_db)
    
    async def train_all_models(self) -> Dict[str, bool]:
        logger.info("[모델 학습 시작] 전체 모델 학습")
        
        results = {
            "lightfm": False,
            "als": False,
            "two_stage": False
        }
        
        interactions = await self.interaction_repo.get_all_for_training()
        total_count = len(interactions)
        
        stats = await self.interaction_repo.get_stats()
        logger.info(f"[학습 데이터] 총 상호작용: {total_count}, 유니크 유저: {stats['unique_users']}, "
                   f"유니크 챌린지: {stats['unique_challenges']}")
        
        if total_count < self.MIN_INTERACTIONS_LIGHTFM:
            logger.info(f"[학습 스킵] 상호작용 부족 ({total_count} < {self.MIN_INTERACTIONS_LIGHTFM})")
            return results
        
        challenges = await self.challenge_repo.find_by_difficulty(max_difficulty_level=3)
        
        training_data = self._prepare_training_data(interactions, challenges)
        
        # LightFM 학습
        if total_count >= self.MIN_INTERACTIONS_LIGHTFM:
            results["lightfm"] = await self._train_lightfm(training_data)
        
        # ALS 학습
        if total_count >= self.MIN_INTERACTIONS_ALS:
            results["als"] = await self._train_als(training_data)
        
        # 2-Stage 학습
        if total_count >= self.MIN_INTERACTIONS_TWO_STAGE:
            results["two_stage"] = await self._train_two_stage(training_data)
        
        logger.info(f"[모델 학습 완료] 결과: {results}")
        return results
    
    def _prepare_training_data(self, interactions: List[Dict], challenges: List[Dict]) -> Dict:
        user_codes = list(set(i["userCode"] for i in interactions))
        challenge_ids = list(set(i["challengeId"] for i in interactions))
        
        user_to_idx = {code: idx for idx, code in enumerate(user_codes)}
        challenge_to_idx = {cid: idx for idx, cid in enumerate(challenge_ids)}
        
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        challenge_map = {_item_code(c): c for c in challenges if _item_code(c) is not None}
        
        return {
            "interactions": interactions,
            "challenges": challenges,
            "user_codes": user_codes,
            "challenge_ids": challenge_ids,
            "user_to_idx": user_to_idx,
            "challenge_to_idx": challenge_to_idx,
            "challenge_map": challenge_map,
            "n_users": len(user_codes),
            "n_items": len(challenge_ids)
        }
    
    # LightFM 모델 학습
    async def _train_lightfm(self, training_data: Dict) -> bool:
        try:
            logger.info("[LightFM 학습 시작]")
            
            interactions_tuples = [
                (i["userCode"], i["challengeId"], i.get("emotion", 3) or 3)
                for i in training_data["interactions"]
                if i.get("interactionType") == "COMPLETED"
            ]
            
            user_features = {}
            for user_code in training_data["user_codes"]:
                user_features[user_code] = [f"user:{user_code}"]
            
            def _item_code(c):
                return c.get("challenge_code") or c.get("challenge_master_id")
            item_features = {}
            for c in training_data["challenges"]:
                cid = _item_code(c)
                if cid is None:
                    continue
                features = [f"category:{c['category']}"]
                features.extend([f"tag:{t}" for t in c.get("tags", [])])
                item_features[cid] = features
            
            model = LightFMRecommender()
            await model.fit(
                interactions=interactions_tuples,
                user_features_data=user_features,
                item_features_data=item_features
            )
            
            metadata = {
                "n_users": training_data["n_users"],
                "n_items": training_data["n_items"],
                "n_interactions": len(interactions_tuples)
            }
            model_manager.save(model, ModelType.LIGHTFM, metadata)
            
            logger.info(f"[LightFM 학습 완료] interactions={len(interactions_tuples)}")
            return True
            
        except Exception as e:
            logger.error(f"[LightFM 학습 실패] {e}")
            return False
    
    # ALS 모델 학습
    async def _train_als(self, training_data: Dict) -> bool:
        try:
            logger.info("[ALS 학습 시작]")
            
            interactions_tuples = [
                (i["userCode"], i["challengeId"], i.get("emotion", 3) or 3)
                for i in training_data["interactions"]
                if i.get("interactionType") == "COMPLETED"
            ]
            
            model = ALSRecommender()
            await model.fit(interactions=interactions_tuples)
            
            metadata = {
                "n_users": training_data["n_users"],
                "n_items": training_data["n_items"],
                "n_interactions": len(interactions_tuples)
            }
            model_manager.save(model, ModelType.ALS, metadata)
            
            logger.info(f"[ALS 학습 완료] interactions={len(interactions_tuples)}")
            return True
            
        except Exception as e:
            logger.error(f"[ALS 학습 실패] {e}")
            return False
    
    # 2-Stage 모델 학습
    async def _train_two_stage(self, training_data: Dict) -> bool:
        try:
            logger.info("[2-Stage 학습 시작]")
            
            interactions_tuples = [
                (i["userCode"], i["challengeId"], i.get("emotion", 3) or 3)
                for i in training_data["interactions"]
                if i.get("interactionType") == "COMPLETED"
            ]
            
            model = TwoStageRecommender()
            await model.fit(
                interactions=interactions_tuples,
                challenges=training_data["challenges"]
            )
            
            metadata = {
                "n_users": training_data["n_users"],
                "n_items": training_data["n_items"],
                "n_interactions": len(interactions_tuples)
            }
            model_manager.save(model, ModelType.TWO_STAGE, metadata)
            
            logger.info(f"[2-Stage 학습 완료] interactions={len(interactions_tuples)}")
            return True
            
        except Exception as e:
            logger.error(f"[2-Stage 학습 실패] {e}")
            return False

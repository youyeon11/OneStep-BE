import numpy as np
from typing import List, Dict, Any, Optional, Set
from dataclasses import dataclass

from lightfm import LightFM
from lightfm.data import Dataset

from app.recommenders.base import BaseRecommender, RecommendationResult
from app.utils.logger import app_logger as logger


@dataclass
class LightFMConfig:
    """LightFM 설정"""
    no_components: int = 32
    learning_rate: float = 0.05
    loss: str = "warp"  # warp, bpr, logistic
    epochs: int = 20
    num_threads: int = 4


# LightFM 추천 알고리즘
class LightFMRecommender(BaseRecommender):

    def __init__(self, config: Optional[LightFMConfig] = None):
        self.config = config or LightFMConfig()
        self.model = LightFM(
            no_components=self.config.no_components,
            loss=self.config.loss,
            learning_rate=self.config.learning_rate
        )
        self.dataset: Optional[Dataset] = None
        self.user_features_matrix = None
        self.item_features_matrix = None
        self.interaction_matrix = None
        self.is_fitted = False
        
        self._user_mapping: Dict[str, int] = {}
        self._item_mapping: Dict[int, int] = {}
        self._reverse_item_mapping: Dict[int, int] = {}
    
    async def fit(
        self,
        interactions: Any,
        user_features_data: Optional[Dict[str, List[str]]] = None,
        item_features_data: Optional[Dict[int, List[str]]] = None,
        **kwargs
    ) -> None:

        # 모델 학습
        epochs = kwargs.get("epochs", self.config.epochs)
        
        logger.info(f"[LightFM] 학습 시작 - interactions: {len(interactions)}, "
                   f"users: {len(user_features_data or {})}, "
                   f"items: {len(item_features_data or {})}")
        
        # 1. Dataset 구성
        self.dataset = Dataset()
        
        user_ids = list(user_features_data.keys()) if user_features_data else []
        item_ids = list(item_features_data.keys()) if item_features_data else []
        
        for user_code, challenge_id, _ in interactions:
            if user_code not in user_ids:
                user_ids.append(user_code)
            if challenge_id not in item_ids:
                item_ids.append(challenge_id)
        
        all_user_features: Set[str] = set()
        if user_features_data:
            for features in user_features_data.values():
                all_user_features.update(features)
        
        all_item_features: Set[str] = set()
        if item_features_data:
            for features in item_features_data.values():
                all_item_features.update(features)
        
        self.dataset.fit(
            users=user_ids,
            items=item_ids,
            user_features=all_user_features if all_user_features else None,
            item_features=all_item_features if all_item_features else None
        )
        
        # 2. Interaction Matrix 구축
        interaction_tuples = [
            (user_code, challenge_id, rating)
            for user_code, challenge_id, rating in interactions
            if rating is not None
        ]
        
        self.interaction_matrix, weights = self.dataset.build_interactions(
            interaction_tuples
        )
        
        # 3. Feature Matrices 구축
        if user_features_data:
            user_feature_tuples = [
                (uid, features) for uid, features in user_features_data.items()
            ]
            self.user_features_matrix = self.dataset.build_user_features(
                user_feature_tuples
            )
        
        if item_features_data:
            item_feature_tuples = [
                (iid, features) for iid, features in item_features_data.items()
            ]
            self.item_features_matrix = self.dataset.build_item_features(
                item_feature_tuples
            )
        
        # 4. Mapping 저장
        user_mapping, _, item_mapping, _ = self.dataset.mapping()
        self._user_mapping = user_mapping
        self._item_mapping = item_mapping
        self._reverse_item_mapping = {v: k for k, v in item_mapping.items()}
        
        # 5. 모델 학습
        self.model.fit(
            self.interaction_matrix,
            user_features=self.user_features_matrix,
            item_features=self.item_features_matrix,
            sample_weight=weights,
            epochs=epochs,
            num_threads=self.config.num_threads,
            verbose=False
        )
        
        self.is_fitted = True
        logger.info(f"[LightFM] 학습 완료 - epochs: {epochs}, "
                   f"components: {self.config.no_components}")
    
    async def recommend(
        self,
        user_features: Dict[str, Any],
        item_pool: List[Dict],
        history: List[Dict],
        top_k: int = 5
    ) -> List[RecommendationResult]:

        # 추천 생성
        if not self.is_fitted:
            logger.warning("[LightFM] 모델이 학습되지 않음. Rule-based Fallback 사용")
            from app.recommenders.rule_based import RuleBasedRecommender
            return await RuleBasedRecommender().recommend(
                user_features, item_pool, history, top_k
            )
        
        user_code = user_features.get("user_code")
        user_idx = self._user_mapping.get(user_code)
        
        if user_idx is None:
            # Cold Start 유저 → Rule-based Fallback
            logger.debug(f"[LightFM] 신규 유저 {user_code} - Rule-based Fallback")
            from app.recommenders.rule_based import RuleBasedRecommender
            return await RuleBasedRecommender().recommend(
                user_features, item_pool, history, top_k
            )

        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        item_ids = [_item_code(item) for item in item_pool if _item_code(item) is not None]
        valid_items = []
        valid_indices = []
        
        for item_id in item_ids:
            item_idx = self._item_mapping.get(item_id)
            if item_idx is not None:
                valid_items.append(item_id)
                valid_indices.append(item_idx)
        
        if not valid_indices:
            logger.warning("[LightFM] 유효한 아이템 없음. Rule-based Fallback 사용")
            from app.recommenders.rule_based import RuleBasedRecommender
            return await RuleBasedRecommender().recommend(
                user_features, item_pool, history, top_k
            )
        
        # LightFM 예측
        item_indices = np.array(valid_indices)
        
        scores = self.model.predict(
            user_ids=user_idx,
            item_ids=item_indices,
            user_features=self.user_features_matrix,
            item_features=self.item_features_matrix
        )
        
        # 점수 정규화 (0~1)
        min_score, max_score = scores.min(), scores.max()
        if max_score > min_score:
            normalized_scores = (scores - min_score) / (max_score - min_score)
        else:
            normalized_scores = np.ones_like(scores) * 0.5
        
        # 히스토리 제외 (최근 완료된 챌린지)
        excluded_ids = self._get_recent_completed_ids(history)
        
        # Top-K 선택
        results = []
        for item_id, score in zip(valid_items, normalized_scores):
            if item_id not in excluded_ids:
                results.append(
                    RecommendationResult(
                        challenge_id=item_id,
                        weight=round(float(score), 4),
                        debug_info={"model": "lightfm", "user_idx": user_idx}
                    )
                )
        
        results.sort(key=lambda x: x.weight, reverse=True)
        
        return results[:top_k]
    
    def _get_recent_completed_ids(
        self, 
        history: List[Dict], 
        days: int = 7
    ) -> Set[int]:
        """최근 완료 챌린지 ID 추출"""
        from datetime import datetime, timedelta
        
        cutoff_date = datetime.now() - timedelta(days=days)
        excluded = set()
        
        for h in history:
            if h.get("challenge_status") != "COMPLETED":
                continue
            
            completed_at = h.get("completed_at")
            if completed_at:
                try:
                    completed_date = datetime.fromisoformat(
                        completed_at.replace('Z', '+00:00')
                    )
                    if completed_date > cutoff_date:
                        mid = h.get("challenge_master_id")
                        if mid is not None:
                            excluded.add(mid)
                except:
                    pass
        
        return excluded
    
    def get_model_info(self) -> Dict[str, Any]:
        """모델 메타정보"""
        return {
            "model_type": "LightFMRecommender",
            "version": "1.0.0",
            "is_fitted": self.is_fitted,
            "config": {
                "no_components": self.config.no_components,
                "loss": self.config.loss,
                "learning_rate": self.config.learning_rate
            }
        }
    
    def save_model(self, path: str) -> None:
        """모델 저장 (pickle)"""
        import pickle
        
        with open(path, "wb") as f:
            pickle.dump({
                "model": self.model,
                "dataset": self.dataset,
                "user_features_matrix": self.user_features_matrix,
                "item_features_matrix": self.item_features_matrix,
                "user_mapping": self._user_mapping,
                "item_mapping": self._item_mapping,
                "config": self.config
            }, f)
        
        logger.info(f"[LightFM] 모델 저장 완료: {path}")
    
    def load_model(self, path: str) -> None:
        """모델 로드 (pickle)"""
        import pickle
        
        with open(path, "rb") as f:
            data = pickle.load(f)
        
        self.model = data["model"]
        self.dataset = data["dataset"]
        self.user_features_matrix = data["user_features_matrix"]
        self.item_features_matrix = data["item_features_matrix"]
        self._user_mapping = data["user_mapping"]
        self._item_mapping = data["item_mapping"]
        self._reverse_item_mapping = {v: k for k, v in self._item_mapping.items()}
        self.config = data["config"]
        self.is_fitted = True
        
        logger.info(f"[LightFM] 모델 로드 완료: {path}")

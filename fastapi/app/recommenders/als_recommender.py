import numpy as np
from typing import List, Dict, Any, Optional, Set, Tuple
from dataclasses import dataclass
from scipy.sparse import csr_matrix
from datetime import datetime, timedelta

from implicit.als import AlternatingLeastSquares

from app.recommenders.base import BaseRecommender, RecommendationResult
from app.utils.logger import app_logger as logger


@dataclass
class ALSConfig:
    factors: int = 64
    iterations: int = 15
    regularization: float = 0.01
    use_gpu: bool = False


# ALS 추천 알고리즘
class ALSRecommender(BaseRecommender):

    def __init__(self, config: Optional[ALSConfig] = None):
        self.config = config or ALSConfig()
        
        self.model = AlternatingLeastSquares(
            factors=self.config.factors,
            iterations=self.config.iterations,
            regularization=self.config.regularization,
            use_gpu=self.config.use_gpu
        )
        
        self.user_mapping: Dict[str, int] = {}
        self.item_mapping: Dict[int, int] = {}
        self.reverse_item_mapping: Dict[int, int] = {}
        self.user_item_matrix: Optional[csr_matrix] = None
        
        self.is_fitted = False
    
    async def fit(
        self,
        interactions: Any,
        **kwargs
    ) -> None:
        # ALS 모델 학습
        logger.info(f"[ALS] 학습 시작 - interactions: {len(interactions)}")
        
        users = list(set(d[0] for d in interactions))
        items = list(set(d[1] for d in interactions))
        
        self.user_mapping = {u: i for i, u in enumerate(users)}
        self.item_mapping = {it: i for i, it in enumerate(items)}
        self.reverse_item_mapping = {v: k for k, v in self.item_mapping.items()}
        
        row_indices = [self.user_mapping[d[0]] for d in interactions]
        col_indices = [self.item_mapping[d[1]] for d in interactions]
        values = [d[2] if len(d) > 2 and d[2] is not None else 1.0 for d in interactions]
        
        self.user_item_matrix = csr_matrix( 
            (values, (row_indices, col_indices)),
            shape=(len(users), len(items))
        )
        
        self.model.fit(self.user_item_matrix)
        
        self.is_fitted = True
        logger.info(f"[ALS] 학습 완료 - users: {len(users)}, items: {len(items)}, "
                   f"factors: {self.config.factors}")
    
    # ALS 추천 생성
    async def recommend(
        self,
        user_features: Dict[str, Any],
        item_pool: List[Dict],
        history: List[Dict],
        top_k: int = 5
    ) -> List[RecommendationResult]:
        if not self.is_fitted:
            logger.warning("[ALS] 모델 미학습 - Rule-based Fallback")
            from app.recommenders.rule_based import RuleBasedRecommender
            return await RuleBasedRecommender().recommend(
                user_features, item_pool, history, top_k
            )
        
        user_code = user_features.get("user_code")
        user_idx = self.user_mapping.get(user_code)
        
        if user_idx is None:
            logger.debug(f"[ALS] Cold Start 유저 {user_code} - Rule-based Fallback")
            from app.recommenders.rule_based import RuleBasedRecommender
            return await RuleBasedRecommender().recommend(
                user_features, item_pool, history, top_k
            )
        
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        valid_items = []
        valid_indices = []
        for item in item_pool:
            item_idx = self.item_mapping.get(_item_code(item))
            if item_idx is not None:
                valid_items.append(item)
                valid_indices.append(item_idx)
        
        if not valid_indices:
            logger.warning("[ALS] 유효한 아이템 없음 - Rule-based Fallback")
            from app.recommenders.rule_based import RuleBasedRecommender
            return await RuleBasedRecommender().recommend(
                user_features, item_pool, history, top_k
            )
        
        user_vector = self.model.user_factors[user_idx]
        item_vectors = self.model.item_factors[valid_indices]
        scores = np.dot(item_vectors, user_vector)
        
        min_score, max_score = scores.min(), scores.max()
        if max_score > min_score:
            normalized = (scores - min_score) / (max_score - min_score)
        else:
            normalized = np.ones_like(scores) * 0.5
        
        excluded_ids = self._get_recent_completed_ids(history)
        
        results = []
        for item, score in zip(valid_items, normalized):
            cid = _item_code(item)
            if cid is not None and cid not in excluded_ids:
                results.append(
                    RecommendationResult(
                        challenge_id=cid,
                        weight=round(float(score), 4),
                        debug_info={"model": "als", "user_idx": user_idx}
                    )
                )
        
        results.sort(key=lambda x: x.weight, reverse=True)
        
        return results[:top_k]
    
    # 최근 완료 챌린지 ID 추출
    def _get_recent_completed_ids(
        self,
        history: List[Dict],
        days: int = 7
    ) -> Set[int]:
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
        return {
            "model_type": "ALSRecommender",
            "version": "1.0.0",
            "is_fitted": self.is_fitted,
            "config": {
                "factors": self.config.factors,
                "iterations": self.config.iterations,
                "regularization": self.config.regularization
            },
            "stats": {
                "num_users": len(self.user_mapping),
                "num_items": len(self.item_mapping)
            }
        }

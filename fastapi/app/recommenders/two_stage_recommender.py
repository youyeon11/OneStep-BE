import numpy as np
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass
from scipy.sparse import csr_matrix

import lightgbm as lgb
from implicit.als import AlternatingLeastSquares

from app.recommenders.base import BaseRecommender, RecommendationResult
from app.utils.logger import app_logger as logger


@dataclass
class TwoStageConfig:
    als_factors: int = 64
    als_iterations: int = 15
    als_regularization: float = 0.01
    
    retrieval_k: int = 100
    cold_start_item_boost: int = 30
    diversity_weight: float = 0.3
    
    lgb_num_leaves: int = 31
    lgb_learning_rate: float = 0.05
    lgb_n_estimators: int = 100


# 2-Stage 추천 알고리즘
class TwoStageRecommender(BaseRecommender):
    
    def __init__(self, config: Optional[TwoStageConfig] = None):
        self.config = config or TwoStageConfig()
        
        # Stage 1: ALS (Retrieval)
        self.retrieval_model = AlternatingLeastSquares(
            factors=self.config.als_factors,
            iterations=self.config.als_iterations,
            regularization=self.config.als_regularization,
            use_gpu=False
        )
        
        # Stage 2: LightGBM (Ranking)
        self.ranking_model: Optional[lgb.Booster] = None
        
        self.user_mapping: Dict[str, int] = {}
        self.item_mapping: Dict[int, int] = {}
        self.reverse_item_mapping: Dict[int, int] = {}
        
        self.user_item_matrix: Optional[csr_matrix] = None
        
        self.is_fitted = False
    
    # 2-Stage 모델 학습
    async def fit(
        self,
        interactions: Any,
        ranking_features: Optional[List[Dict]] = None,
        ranking_labels: Optional[List[float]] = None,
        **kwargs
    ) -> None:
        logger.info(f"[2-Stage] 학습 시작 - interactions: {len(interactions)}")
        
        await self._fit_als(interactions)
        
        if ranking_features and ranking_labels:
            await self._fit_lgb(ranking_features, ranking_labels)
        else:
            logger.info("[2-Stage] Ranking 피처 없음 - ALS 점수만 사용")
        
        self.is_fitted = True
        logger.info("[2-Stage] 학습 완료")
    
    # ALS 모델 학습
    async def _fit_als(self, interactions: List[Tuple]) -> None:
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
        
        self.retrieval_model.fit(self.user_item_matrix)
        
        logger.info(f"[ALS] 학습 완료 - users: {len(users)}, items: {len(items)}")
    
    # LightGBM 모델 학습
    async def _fit_lgb(
        self,
        features: List[Dict],
        labels: List[float]
    ) -> None:
        feature_names = list(features[0].keys()) if features else []
        X = np.array([[f.get(name, 0) for name in feature_names] for f in features])
        y = np.array(labels)
        
        train_data = lgb.Dataset(X, label=y, feature_name=feature_names)
        
        params = {
            'objective': 'regression',
            'metric': 'rmse',
            'num_leaves': self.config.lgb_num_leaves,
            'learning_rate': self.config.lgb_learning_rate,
            'verbose': -1
        }
        
        self.ranking_model = lgb.train(
            params,
            train_data,
            num_boost_round=self.config.lgb_n_estimators
        )
        
        logger.info(f"[LightGBM] 학습 완료 - features: {len(feature_names)}")
    
    # 2-Stage 추천 생성
    async def recommend(
        self,
        user_features: Dict[str, Any],
        item_pool: List[Dict],
        history: List[Dict],
        top_k: int = 5
    ) -> List[RecommendationResult]:
        if not self.is_fitted:
            logger.warning("[2-Stage] 모델 미학습 - Rule-based Fallback")
            from app.recommenders.rule_based import RuleBasedRecommender
            return await RuleBasedRecommender().recommend(
                user_features, item_pool, history, top_k
            )
        
        user_code = user_features.get("user_code")
        
        # 1단계: Retrieval (ALS)
        candidates = await self._retrieve(user_code, item_pool)
        
        if not candidates:
            logger.debug(f"[2-Stage] 후보 없음 - Rule-based Fallback")
            from app.recommenders.rule_based import RuleBasedRecommender
            return await RuleBasedRecommender().recommend(
                user_features, item_pool, history, top_k
            )
        
        # 2단계: Ranking (LightGBM)
        ranked = await self._rank(candidates, user_features, history)
        
        return ranked[:top_k]
    
    async def _retrieve(
        self,
        user_code: str,
        item_pool: List[Dict]
    ) -> List[Dict]:
        user_idx = self.user_mapping.get(user_code)
        
        logger.info(f"[_retrieve] user_code: {user_code}, user_idx: {user_idx}, item_pool size: {len(item_pool)}")
        
        als_scored_items = []
        cold_start_items = []
        
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        for item in item_pool:
            item_idx = self.item_mapping.get(_item_code(item))
            item_copy = item.copy()
            
            if item_idx is not None and user_idx is not None:
                user_vector = self.retrieval_model.user_factors[user_idx]
                item_vector = self.retrieval_model.item_factors[item_idx]
                score = float(np.dot(item_vector, user_vector))
                
                item_copy["als_score"] = score
                item_copy["is_cold_start"] = False
                als_scored_items.append(item_copy)
            else:
                item_copy["als_score"] = 0.0
                item_copy["is_cold_start"] = True
                cold_start_items.append(item_copy)
        
        if user_idx is None:
            logger.debug(f"[ALS] Cold Start 유저 {user_code} - 모든 아이템 반환")
            return (als_scored_items + cold_start_items)[:self.config.retrieval_k]
        
        als_scored_items.sort(key=lambda x: x["als_score"], reverse=True)
        
        num_als = min(
            self.config.retrieval_k - self.config.cold_start_item_boost,
            len(als_scored_items)
        )
        num_cold = min(
            self.config.cold_start_item_boost,
            len(cold_start_items)
        )
        
        candidates = als_scored_items[:num_als] + cold_start_items[:num_cold]
        
        logger.info(f"[ALS] 후보 추출 완료 - total: {len(candidates)}, ALS: {num_als}/{len(als_scored_items)}, Cold: {num_cold}/{len(cold_start_items)}, config.retrieval_k: {self.config.retrieval_k}, config.cold_start_item_boost: {self.config.cold_start_item_boost}")
        return candidates
    
    async def _rank(
        self,
        candidates: List[Dict],
        user_features: Dict[str, Any],
        history: List[Dict]
    ) -> List[RecommendationResult]:
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        results = []
        
        if self.ranking_model is None:
            for c in candidates:
                als_score = c.get("als_score", 0.5)
                normalized = (als_score + 1) / 2 if als_score < 0 else min(als_score, 1.0)
                results.append(
                    RecommendationResult(
                        challenge_id=_item_code(c),
                        weight=round(normalized, 4),
                        debug_info={"model": "two_stage", "stage": "als_only"}
                    )
                )
        else:
            features = []
            for c in candidates:
                feat = self._build_ranking_features(c, user_features, history)
                features.append(feat)
            
            feature_names = list(features[0].keys()) if features else []
            X = np.array([[f.get(name, 0) for name in feature_names] for f in features])
            
            lgb_scores = self.ranking_model.predict(X)
            
            for c, score in zip(candidates, lgb_scores):
                results.append(
                    RecommendationResult(
                        challenge_id=_item_code(c),
                        weight=round(float(score), 4),
                        debug_info={
                            "model": "two_stage",
                            "als_score": c.get("als_score", 0),
                            "lgb_score": float(score)
                        }
                    )
                )
        
        results.sort(key=lambda x: x.weight, reverse=True)
        return results
    
    # 랭킹용 피처 생성
    def _build_ranking_features(
        self,
        candidate: Dict,
        user_features: Dict[str, Any],
        history: List[Dict]
    ) -> Dict[str, float]:
        challenge_id = candidate.get("challenge_code") or candidate.get("challenge_master_id")
        if challenge_id is None:
            challenge_id = 0
        features = {
            "als_score": candidate.get("als_score", 0.0),
            "is_cold_start": 1.0 if candidate.get("is_cold_start", False) else 0.0,
            "difficulty": candidate.get("difficulty_level", 2),
            "recovery_level": user_features.get("recovery_level", 2),
        }
        
        preferred_categories = set(user_features.get("preferred_categories", []))
        item_category = candidate.get("category", "")
        features["category_match"] = 1.0 if item_category in preferred_categories else 0.0
        
        user_tags = set(user_features.get("interest_tags", []))
        item_tags = set(candidate.get("tags", []))
        overlap = len(user_tags & item_tags)
        features["tag_overlap"] = overlap / max(len(user_tags), 1)
        
        def _master_id(h):
            return h.get("challenge_master_id")
        completed_ids = {_master_id(h) for h in history if h.get("challenge_status") == "COMPLETED" and _master_id(h) is not None}
        features["was_completed"] = 1.0 if challenge_id in completed_ids else 0.0
        
        emotions = [h.get("emotion", 3) for h in history if _master_id(h) == challenge_id]
        features["avg_emotion"] = sum(emotions) / len(emotions) if emotions else 3.0
        
        if candidate.get("is_cold_start", False):
            content_score = (
                features["category_match"] * 0.4 +
                features["tag_overlap"] * 0.6
            )
            features["als_score"] = content_score * 0.5
        
        return features
    
    def get_model_info(self) -> Dict[str, Any]:
        return {
            "model_type": "TwoStageRecommender",
            "version": "1.0.0",
            "is_fitted": self.is_fitted,
            "config": {
                "als_factors": self.config.als_factors,
                "retrieval_k": self.config.retrieval_k,
                "lgb_n_estimators": self.config.lgb_n_estimators
            },
            "stats": {
                "num_users": len(self.user_mapping),
                "num_items": len(self.item_mapping),
                "has_ranking_model": self.ranking_model is not None
            }
        }

from typing import List, Dict
import numpy as np
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.schemas.initial_recommendation_schemas import (
    InitialRecommendationRequest,
    InitialRecommendationResult,
    RecommendationItem,
    RecommendationMetadata,
    SurveyAnswer
)
from app.repositories.challenge_repository import ChallengeRepository
from app.utils.logger import app_logger as logger


# 초기 챌린지 추천 서비스(CBF 알고리즘)
class InitialRecommendationService:
    TARGET_COUNT = 20
    
    def __init__(self, mongo_db: AsyncIOMotorDatabase):
        self.repository = ChallengeRepository(mongo_db)
    
    async def recommend(
        self,
        request: InitialRecommendationRequest
    ) -> InitialRecommendationResult:
        logger.info(f"[초기 추천 시작] user_code={request.user_code}, "
                   f"recovery_level={request.recovery_level}, "
                   f"responses_count={len(request.survey_responses)}")
        
        category_scores = self._calculate_category_scores(request.survey_responses)
        logger.info(f"카테고리별 점수: {category_scores}")
        
        all_challenges = await self.repository.find_by_difficulty(
            max_difficulty_level=request.recovery_level
        )
        
        logger.info(f"조회된 챌린지 수: {len(all_challenges)} (max_difficulty: {request.recovery_level})")
        
        if len(all_challenges) < self.TARGET_COUNT:
            logger.warning(f"챌린지 부족: {len(all_challenges)}개 (필요: {self.TARGET_COUNT}개)")
            raise ValueError(f"추천 가능한 챌린지가 부족합니다. (필요: {self.TARGET_COUNT}개, 현재: {len(all_challenges)}개)")
        
        recommendations = self._calculate_weights(
            all_challenges,
            category_scores,
            request.recovery_level
        )
        
        # 카테고리별 최소 3개 + 나머지 가중치별 랜덤 샘플링
        selected_recommendations = self._balanced_sampling(
            recommendations,
            all_challenges,
            self.TARGET_COUNT
        )
        
        logger.info(f"[초기 추천 완료] user_code={request.user_code}, "
                   f"recommendations_count={len(selected_recommendations)}, "
                   f"weight_range=({min(r.weight for r in selected_recommendations):.4f} ~ "
                   f"{max(r.weight for r in selected_recommendations):.4f})")
        
        return InitialRecommendationResult(
            recommendations=selected_recommendations,
            metadata=RecommendationMetadata()
        )
    
    def _calculate_category_scores(
        self,
        survey_responses: List[SurveyAnswer]
    ) -> Dict[str, float]:
        """카테고리별 평균 점수 계산"""
        category_responses: Dict[str, List[int]] = {}
        
        for response in survey_responses:
            if response.tag not in category_responses:
                category_responses[response.tag] = []
            category_responses[response.tag].append(response.answer)
        
        return {
            category: round(sum(answers) / len(answers), 2)
            for category, answers in category_responses.items()
        }
    
    def _calculate_weights(
        self,
        challenges: List[Dict],
        category_scores: Dict[str, float],
        recovery_level: int
    ) -> List[RecommendationItem]:
        """
        CBF 가중치 계산
        - 난이도 매칭: recovery_level과 가까울수록 (0.0 ~ 0.5)
        - 카테고리 점수: 점수 높을수록 높은 가중치 (0.1 ~ 0.3)
        
        Note: survey answer 범위는 0~3 (PostgreSQL survey_log.answer와 동일)
        """
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        recommendations = []
        
        for challenge in challenges:
            cid = _item_code(challenge)
            if cid is None:
                continue
            category = challenge["category"]
            difficulty = challenge["difficulty_level"]
            
            if category in category_scores:
                score = category_scores[category]  # 0~3 범위
                category_score = 0.1 + (score / 3.0) * 0.2  # 정규화: 0~3 → 0~1
            else:
                category_score = 0.05
            
            difficulty_diff = abs(difficulty - recovery_level)
            difficulty_score = (3 - difficulty_diff) / 3 * 0.5
            
            weight = min(category_score + difficulty_score, 1.0)
            weight = round(weight, 4)
            
            recommendations.append(
                RecommendationItem(
                    challenge_id=cid,
                    weight=weight
                )
            )
        
        return recommendations
    
    def _balanced_sampling(
        self,
        recommendations: List[RecommendationItem],
        challenges: List[Dict],
        count: int
    ) -> List[RecommendationItem]:

        MIN_PER_CATEGORY = 3
        CATEGORIES = ['LIFESTYLE', 'SOCIAL', 'INNER']
        
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        challenge_category_map = {
            _item_code(c): c["category"]
            for c in challenges
            if _item_code(c) is not None
        }
        
        # 카테고리별로 추천 그룹화
        category_recommendations = {cat: [] for cat in CATEGORIES}
        for rec in recommendations:
            category = challenge_category_map.get(rec.challenge_id)
            if category in category_recommendations:
                category_recommendations[category].append(rec)
        
        selected = []
        selected_ids = set()
        
        # 각 카테고리에서 최소 3개 선택
        for category in CATEGORIES:
            cat_recs = category_recommendations[category]
            
            if len(cat_recs) < MIN_PER_CATEGORY:
                logger.warning(f"카테고리 {category} 챌린지 부족: {len(cat_recs)}개")
                selected.extend(cat_recs)
                selected_ids.update(r.challenge_id for r in cat_recs)
            else:
                sampled = self._weighted_random_sampling(cat_recs, MIN_PER_CATEGORY)
                selected.extend(sampled)
                selected_ids.update(r.challenge_id for r in sampled)
        
        # 나머지 챌린지를 전체에서 가중치별 랜덤 샘플링
        remaining_count = count - len(selected)
        if remaining_count > 0:
            remaining_recs = [r for r in recommendations if r.challenge_id not in selected_ids]
            
            if remaining_recs:
                additional = self._weighted_random_sampling(
                    remaining_recs,
                    min(remaining_count, len(remaining_recs))
                )
                selected.extend(additional)
        
        return selected[:count]
    
    def _weighted_random_sampling(
        self,
        recommendations: List[RecommendationItem],
        count: int
    ) -> List[RecommendationItem]:
        """가중치별 랜덤 샘플링"""
        if not recommendations:
            return []
        
        if len(recommendations) <= count:
            return recommendations
        
        weights = np.array([r.weight for r in recommendations])
        probabilities = weights / weights.sum()
        
        selected_indices = np.random.choice(
            len(recommendations),
            size=count,
            replace=False,
            p=probabilities
        )
        
        return [recommendations[i] for i in selected_indices]

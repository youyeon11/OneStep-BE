from typing import List, Dict, Any
from datetime import datetime, timedelta
import random
from app.recommenders.base import BaseRecommender, RecommendationResult


# 규칙 기반 추천 알고리즘
class RuleBasedRecommender(BaseRecommender):
    """
    Rule-based CBF + 동적 행동 가중치 + 확률 샘플링
    
    특징:
    - 빠름, 해석 가능, Cold Start 대응
    - 행동 로그 축적에 따라 동적으로 가중치 조정
    - 확률 기반 샘플링으로 다양성 확보
    - 음수 가중치로 부정적 피드백 반영
    
    알고리즘:
    - CBF: 태그(40%) + 카테고리(30%) + 난이도(30%)
    - 행동 가중치: 챌린지 자체(35%) + 태그(35%) + 카테고리(30%)
      * emotion: 5(+0.4), 4(+0.2), 3(0.0), 2(-0.2), 1(-0.4)
      * ASSIGNED(추천받았지만 안 함): -0.2 (관심 없음)
    - 동적 비율: 완료 챌린지 개수에 따라 CBF ↔ 행동 가중치 조정
    - 최근 완료 페널티: 7일(×0.3), 14일(×0.7), 14일+(×1.0)
    - 확률 샘플링: weight에 비례한 확률로 선택
    """
    
    async def fit(self, interactions: Any, **kwargs) -> None:
        pass
    
    async def recommend(
        self,
        user_features: Dict[str, Any],
        item_pool: List[Dict],
        history: List[Dict],
        top_k: int = 5
    ) -> List[RecommendationResult]:
        
        # 1. 완료 챌린지 개수에 따라 동적 가중치 계산
        completed_count = len([h for h in history if h.get("challenge_status") == "COMPLETED"])
        cbf_weight, behavior_weight = self._calculate_dynamic_weights(completed_count)
        
        # 2. CBF 점수 계산
        cbf_scores = self._calculate_cbf_scores(item_pool, user_features)
        
        # 3. 행동 가중치 계산
        behavior_weights = self._calculate_behavior_weights(item_pool, history)
        
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        
        # 4. 최종 점수 합산 (동적 가중치 적용)
        final_scores = {}
        for item in item_pool:
            cid = _item_code(item)
            if cid is None:
                continue
            cbf = cbf_scores.get(cid, 0.0)
            behavior = behavior_weights.get(cid, 0.0)
            
            final_scores[cid] = cbf * cbf_weight + behavior * behavior_weight
        
        # 5. 최근 완료 페널티 적용 (다양성 확보)
        final_scores = self._apply_recency_penalty(final_scores, history)
        
        # 6. 음수 점수 제거 (추천 불가)
        final_scores = {cid: max(score, 0.01) for cid, score in final_scores.items()}
        
        # 7. 확률 기반 샘플링 (다양성 확보)
        selected = self._weighted_random_sampling(final_scores, item_pool, top_k)
        
        return [
            RecommendationResult(
                challenge_id=_item_code(item),
                weight=round(final_scores[_item_code(item)], 4),
                debug_info={
                    "cbf": cbf_scores.get(_item_code(item)), 
                    "behavior": behavior_weights.get(_item_code(item)),
                    "cbf_weight": cbf_weight,
                    "behavior_weight": behavior_weight,
                    "completed_count": completed_count
                }
            )
            for item in selected
        ]
    
    def _calculate_dynamic_weights(self, completed_count: int) -> tuple:
        """
        완료 챌린지 개수에 따라 동적으로 가중치 조정
        
        Returns:
            (cbf_weight, behavior_weight): CBF와 행동 가중치
        """
        if completed_count == 0:
            return (1.0, 0.0)
        elif completed_count < 5:
            return (0.9, 0.1)
        elif completed_count < 10:
            return (0.8, 0.2)
        elif completed_count < 20:
            return (0.6, 0.4)
        else:
            return (0.4, 0.6)
    
    def _calculate_cbf_scores(
        self,
        challenges: List[Dict],
        user_features: Dict[str, Any]
    ) -> Dict[int, float]:
        interest_tags = user_features.get("interest_tags", [])
        preferred_categories = user_features.get("preferred_categories", [])
        recovery_level = user_features.get("recovery_level", 2)
        
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        scores = {}
        for c in challenges:
            cid = _item_code(c)
            if cid is None:
                continue
            c_tags = set(c.get("tags", []))
            i_tags = set(interest_tags)
            tag_score = len(c_tags & i_tags) / len(c_tags | i_tags) if (c_tags | i_tags) else 0.0
            
            item_category = c.get("category", "")
            category_score = 1.0 if item_category in preferred_categories else 0.3
            
            diff = abs(c.get("difficulty_level", 2) - recovery_level)
            difficulty_score = (3 - diff) / 3
            
            scores[cid] = round(
                tag_score * 0.4 + category_score * 0.3 + difficulty_score * 0.3, 4
            )
        return scores
    
    def _calculate_behavior_weights(
        self, 
        challenges: List[Dict], 
        history: List[Dict]
    ) -> Dict[int, float]:
        if not history:
            return {}
        
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        challenge_map = {_item_code(c): c for c in challenges if _item_code(c) is not None}
        
        def _master_id(h):
            return h.get("challenge_master_id")
        
        # 1. 챌린지 자체 가중치 (item_pool의 challenge_code/challenge_master_id = 챌린지 테이블 ID)
        challenge_scores = {}
        for h in history:
            cid = _master_id(h)
            if cid is None:
                continue
            
            if h.get("challenge_status") == "COMPLETED":
                emotion = h.get("emotion", 3)
                normalized_score = (emotion - 3) * 0.2
                challenge_scores[cid] = max(
                    challenge_scores.get(cid, -1.0), 
                    normalized_score
                )
            elif h.get("challenge_status") == "ASSIGNED":
                if cid not in challenge_scores:
                    challenge_scores[cid] = -0.2
        
        # 2. 카테고리 선호도
        category_pref = {}
        for h in history:
            if h.get("challenge_status") != "COMPLETED":
                continue
            cid = _master_id(h)
            if cid is None:
                continue
            challenge_info = challenge_map.get(cid)
            if not challenge_info:
                continue
            
            cat = challenge_info.get("category")
            if cat:
                if cat not in category_pref:
                    category_pref[cat] = {"count": 0, "total": 0.0}
                category_pref[cat]["count"] += 1
                emotion = h.get("emotion", 3)
                normalized_score = (emotion - 3) * 0.2
                category_pref[cat]["total"] += normalized_score
        
        # 3. 태그 선호도
        tag_pref = {}
        for h in history:
            if h.get("challenge_status") != "COMPLETED":
                continue
            cid = _master_id(h)
            if cid is None:
                continue
            challenge_info = challenge_map.get(cid)
            if not challenge_info:
                continue
            
            emotion = h.get("emotion", 3)
            normalized_score = (emotion - 3) * 0.2
            
            for tag in challenge_info.get("tags", []):
                tag_pref[tag] = tag_pref.get(tag, 0) + normalized_score
        
        # 최종 계산
        weights = {}
        for c in challenges:
            cid = _item_code(c)
            if cid is None:
                continue
            score = 0.0
            
            # (1) 챌린지 자체 (35%)
            if cid in challenge_scores:
                score += challenge_scores[cid] * 0.35
            
            # (2) 태그 (35%)
            c_tags = c.get("tags", [])
            if c_tags and tag_pref:
                tag_score = sum(tag_pref.get(t, 0) for t in c_tags) / len(c_tags)
                score += tag_score * 0.35
            
            # (3) 카테고리 (30%)
            cat = c["category"]
            if cat in category_pref:
                avg = category_pref[cat]["total"] / category_pref[cat]["count"]
                boost = min(1.0 + abs(category_pref[cat]["count"]) * 0.05, 1.3)
                score += (avg * boost) * 0.3
            
            weights[cid] = round(score, 4)
        
        return weights

    # 최근 완료 페널티 적용 (다양성 확보)
    def _apply_recency_penalty(
        self, 
        scores: Dict[int, float], 
        history: List[Dict]
    ) -> Dict[int, float]:
        today = datetime.now().date()
        
        completed_recent = {}
        for h in history:
            if h.get("challenge_status") != "COMPLETED":
                continue
            
            completed_at = h.get("completed_at")
            if not completed_at:
                continue
            
            try:
                completed_date = datetime.fromisoformat(completed_at.replace('Z', '+00:00')).date()
                days_ago = (today - completed_date).days
                cid = h.get("challenge_master_id")
                if cid is not None and (cid not in completed_recent or days_ago < completed_recent[cid]):
                    completed_recent[cid] = days_ago
            except Exception:
                continue
        
        penalized_scores = {}
        for cid, score in scores.items():
            if cid in completed_recent:
                days_ago = completed_recent[cid]
                if days_ago <= 7:
                    penalty = 0.3
                elif days_ago <= 14:
                    penalty = 0.7
                else:
                    penalty = 1.0
                penalized_scores[cid] = score * penalty
            else:
                penalized_scores[cid] = score
        
        return penalized_scores
    
    # 확률 기반 샘플링 (다양성 확보)
    def _weighted_random_sampling(
        self,
        scores: Dict[int, float],
        item_pool: List[Dict],
        k: int
    ) -> List[Dict]:
        if not scores:
            return random.sample(item_pool, min(k, len(item_pool)))
        
        def _item_code(c):
            return c.get("challenge_code") or c.get("challenge_master_id")
        challenge_map = {_item_code(c): c for c in item_pool if _item_code(c) is not None}
        
        candidates = [(cid, score) for cid, score in scores.items() if score > 0]
        if not candidates:
            return random.sample(item_pool, min(k, len(item_pool)))
        
        candidates.sort(key=lambda x: x[1], reverse=True)
        
        top_candidates = candidates[:min(k * 3, len(candidates))]
        
        challenge_ids = [cid for cid, _ in top_candidates]
        weights = [score for _, score in top_candidates]
        
        selected_count = min(k, len(challenge_ids))
        selected_ids = random.choices(
            challenge_ids,
            weights=weights,
            k=selected_count
        )
        
        seen = set()
        unique_selected = []
        for cid in selected_ids:
            if cid not in seen:
                seen.add(cid)
                unique_selected.append(challenge_map[cid])
        
        while len(unique_selected) < k and len(unique_selected) < len(item_pool):
            remaining = [c for c in item_pool if _item_code(c) not in seen]
            if not remaining:
                break
            selected = random.choice(remaining)
            seen.add(_item_code(selected))
            unique_selected.append(selected)
        
        return unique_selected[:k]

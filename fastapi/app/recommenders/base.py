from abc import ABC, abstractmethod
from typing import List, Dict, Any
from dataclasses import dataclass, field


@dataclass
class RecommendationResult:
    challenge_id: int
    weight: float
    debug_info: Dict[str, Any] = field(default_factory=dict)


# 추천 모델 인터페이스(Strategy Pattern)
class BaseRecommender(ABC):
    
    @abstractmethod
    async def recommend(
        self,
        user_features: Dict[str, Any],
        item_pool: List[Dict],
        history: List[Dict],
        top_k: int = 5
    ) -> List[RecommendationResult]:
        pass
    
    @abstractmethod
    async def fit(self, interactions: Any, **kwargs) -> None:
        pass
    
    # 모델 메타정보 반환
    def get_model_info(self) -> Dict[str, Any]:
        return {
            "model_type": self.__class__.__name__,
            "version": "1.0.0",
            "description": self.__doc__
        }

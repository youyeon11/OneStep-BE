from enum import Enum
from typing import Dict

from app.recommenders.base import BaseRecommender


class ModelType(Enum):
    RULE_BASED = "rule_based"
    LIGHTFM = "lightfm"
    ALS = "als"
    TWO_STAGE = "two_stage"


class RecommenderFactory:
    
    _recommenders: Dict[ModelType, BaseRecommender] = {}
    
    @classmethod
    def get_recommender(cls, model_type: ModelType) -> BaseRecommender:
        if model_type not in cls._recommenders:
            cls._recommenders[model_type] = cls._create_recommender(model_type)
        return cls._recommenders[model_type]
    
    @classmethod
    def _create_recommender(cls, model_type: ModelType) -> BaseRecommender:
        if model_type == ModelType.RULE_BASED:
            from app.recommenders.rule_based import RuleBasedRecommender
            return RuleBasedRecommender()
        elif model_type == ModelType.LIGHTFM:
            from app.recommenders.lightfm_recommender import LightFMRecommender
            return LightFMRecommender()
        elif model_type == ModelType.ALS:
            from app.recommenders.als_recommender import ALSRecommender
            return ALSRecommender()
        elif model_type == ModelType.TWO_STAGE:
            from app.recommenders.two_stage_recommender import TwoStageRecommender
            return TwoStageRecommender()
        else:
            raise ValueError(f"Unknown model type: {model_type}")
    
    @classmethod
    def clear_cache(cls) -> None:
        cls._recommenders.clear()

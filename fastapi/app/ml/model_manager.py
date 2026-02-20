import os
import pickle
from typing import Optional, Dict
from datetime import datetime

from app.recommenders.factory import ModelType
from app.recommenders.base import BaseRecommender
from app.utils.logger import app_logger as logger


# 모델 관리자
class ModelManager:
    
    MODEL_DIR = os.getenv("MODEL_DIR", "/app/models")
    
    # 모델 선택 임계치
    THRESHOLDS = {
        "lightfm_min_interactions": 1000,
        "lightfm_min_user_interactions": 5,
        "als_min_interactions": 5000,
        "als_min_user_interactions": 10,
        "two_stage_min_interactions": 10000,
        "two_stage_min_user_interactions": 20
    }
    
    def __init__(self):
        os.makedirs(self.MODEL_DIR, exist_ok=True)
        self._model_cache: Dict[ModelType, BaseRecommender] = {}
        self._model_metadata: Dict[ModelType, Dict] = {}
    
    def _get_model_path(self, model_type: ModelType) -> str:
        return os.path.join(self.MODEL_DIR, f"{model_type.value}_model.pkl")
    
    def _get_metadata_path(self, model_type: ModelType) -> str:
        return os.path.join(self.MODEL_DIR, f"{model_type.value}_metadata.pkl")
    
    def save(self, model: BaseRecommender, model_type: ModelType, metadata: Dict = None) -> bool:
        try:
            model_path = self._get_model_path(model_type)
            with open(model_path, 'wb') as f:
                pickle.dump(model, f)
            
            meta = metadata or {}
            meta["saved_at"] = datetime.now().isoformat()
            meta["model_type"] = model_type.value
            
            metadata_path = self._get_metadata_path(model_type)
            with open(metadata_path, 'wb') as f:
                pickle.dump(meta, f)
            
            self._model_cache[model_type] = model
            self._model_metadata[model_type] = meta
            
            logger.info(f"[모델 저장 완료] type={model_type.value}, path={model_path}")
            return True
            
        except Exception as e:
            logger.error(f"[모델 저장 실패] type={model_type.value}, error={e}")
            return False
    
    def load(self, model_type: ModelType) -> Optional[BaseRecommender]:
        if model_type in self._model_cache:
            return self._model_cache[model_type]
        
        model_path = self._get_model_path(model_type)
        if not os.path.exists(model_path):
            logger.debug(f"[모델 파일 없음] type={model_type.value}")
            return None
        
        try:
            with open(model_path, 'rb') as f:
                model = pickle.load(f)
            
            metadata_path = self._get_metadata_path(model_type)
            if os.path.exists(metadata_path):
                with open(metadata_path, 'rb') as f:
                    self._model_metadata[model_type] = pickle.load(f)
            
            self._model_cache[model_type] = model
            
            logger.info(f"[모델 로드 완료] type={model_type.value}")
            return model
            
        except Exception as e:
            logger.error(f"[모델 로드 실패] type={model_type.value}, error={e}")
            return None
    
    def exists(self, model_type: ModelType) -> bool:
        return os.path.exists(self._get_model_path(model_type))
    
    def get_metadata(self, model_type: ModelType) -> Optional[Dict]:
        if model_type in self._model_metadata:
            return self._model_metadata[model_type]
        
        metadata_path = self._get_metadata_path(model_type)
        if os.path.exists(metadata_path):
            with open(metadata_path, 'rb') as f:
                return pickle.load(f)
        
        return None
    
    def clear_cache(self, model_type: ModelType = None):
        if model_type:
            self._model_cache.pop(model_type, None)
            self._model_metadata.pop(model_type, None)
        else:
            self._model_cache.clear()
            self._model_metadata.clear()
    
    async def select_model_type(
        self, 
        total_interactions: int, 
        user_interactions: int
    ) -> ModelType:
        
        # 2-Stage: 대규모 데이터
        if (total_interactions >= self.THRESHOLDS["two_stage_min_interactions"] and
            user_interactions >= self.THRESHOLDS["two_stage_min_user_interactions"] and
            self.exists(ModelType.TWO_STAGE)):
            return ModelType.TWO_STAGE
        
        # ALS: 중간 규모 (협업 필터링)
        if (total_interactions >= self.THRESHOLDS["als_min_interactions"] and
            user_interactions >= self.THRESHOLDS["als_min_user_interactions"] and
            self.exists(ModelType.ALS)):
            return ModelType.ALS
        
        # LightFM: 소규모 (하이브리드)
        if (total_interactions >= self.THRESHOLDS["lightfm_min_interactions"] and
            user_interactions >= self.THRESHOLDS["lightfm_min_user_interactions"] and
            self.exists(ModelType.LIGHTFM)):
            return ModelType.LIGHTFM
        
        # 기본: Rule-based
        return ModelType.RULE_BASED
    
    def get_available_models(self) -> Dict[str, bool]:
        return {
            model_type.value: self.exists(model_type)
            for model_type in [ModelType.LIGHTFM, ModelType.ALS, ModelType.TWO_STAGE]
        }


model_manager = ModelManager()

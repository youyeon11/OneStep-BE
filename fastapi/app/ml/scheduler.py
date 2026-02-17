import asyncio
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.ml.model_trainer import ModelTrainer
from app.repositories.interaction_repository import InteractionRepository
from app.utils.logger import app_logger as logger


# 배치 처리 후 조건부 모델 학습 관리
class MLTrainingManager:
    
    # 학습 트리거 임계치
    MIN_INTERACTIONS_FOR_TRAINING = 500
    TRAINING_INTERVAL_INTERACTIONS = 500  # 마지막 학습 이후 추가 상호작용
    
    def __init__(self):
        self.mongo_db = None
        self._last_training_count = 0
        self._is_training = False
    
    def configure(self, mongo_db: AsyncIOMotorDatabase):
        self.mongo_db = mongo_db
    
    async def check_and_train(self) -> bool:
        if self.mongo_db is None:
            return False
        
        if self._is_training:
            logger.debug("[ML] 이미 학습 진행 중")
            return False
        
        try:
            interaction_repo = InteractionRepository(self.mongo_db)
            current_count = await interaction_repo.count_completed()
            
            # 최소 임계치 미달
            if current_count < self.MIN_INTERACTIONS_FOR_TRAINING:
                logger.debug(f"[ML] 학습 스킵 - 상호작용 부족 ({current_count} < {self.MIN_INTERACTIONS_FOR_TRAINING})")
                return False
            
            # 마지막 학습 이후 충분한 새 데이터가 쌓였는지 확인
            new_interactions = current_count - self._last_training_count
            if new_interactions < self.TRAINING_INTERVAL_INTERACTIONS:
                logger.debug(f"[ML] 학습 스킵 - 새 상호작용 부족 ({new_interactions} < {self.TRAINING_INTERVAL_INTERACTIONS})")
                return False
            
            # 학습 실행
            self._is_training = True
            logger.info(f"[ML] 학습 조건 충족 - 현재: {current_count}, 신규: {new_interactions}")
            
            trainer = ModelTrainer(self.mongo_db)
            results = await trainer.train_all_models()
            
            self._last_training_count = current_count
            logger.info(f"[ML] 학습 완료: {results}")
            
            return any(results.values())
            
        except Exception as e:
            logger.error(f"[ML] 학습 실패: {e}")
            return False
        finally:
            self._is_training = False
    
    async def force_train(self) -> dict:
        """수동 학습 트리거"""
        if self.mongo_db is None:
            return {"error": "MongoDB 연결 없음"}
        
        logger.info("[ML] 수동 학습 트리거")
        
        try:
            trainer = ModelTrainer(self.mongo_db)
            results = await trainer.train_all_models()
            
            interaction_repo = InteractionRepository(self.mongo_db)
            self._last_training_count = await interaction_repo.count_completed()
            
            return results
        except Exception as e:
            logger.error(f"[ML] 수동 학습 실패: {e}")
            return {"error": str(e)}
    
    def start(self):
        """main.py 호환용 - 배치 트리거 방식이므로 별도 시작 불필요"""
        logger.info("[ML] 배치 트리거 모드 활성화 (상호작용 500개마다 자동 학습)")
    
    def stop(self):
        """main.py 호환용"""
        logger.info("[ML] 종료")
    
    def get_next_run_time(self) -> str:
        return "배치 트리거 방식 (고정 스케줄 없음)"
    
    def get_status(self) -> dict:
        return {
            "mode": "batch_triggered",
            "is_training": self._is_training,
            "last_training_count": self._last_training_count,
            "min_threshold": self.MIN_INTERACTIONS_FOR_TRAINING,
            "training_interval": self.TRAINING_INTERVAL_INTERACTIONS
        }


ml_scheduler = MLTrainingManager()

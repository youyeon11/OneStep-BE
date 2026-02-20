from fastapi import HTTPException
from app.exceptions.error_code import ErrorCode

class BaseAPIException(HTTPException):
    """API 예외 기본 클래스"""
    def __init__(self, error_code: ErrorCode):
        self.error_code = error_code
        super().__init__(
            status_code=error_code.status_code,
            detail=error_code.message
        )

class UnauthorizedException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.UNAUTHORIZED)

class UserNotFoundException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.USER_NOT_FOUND)

class InsufficientDataException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.INSUFFICIENT_DATA)

class NoAvailableChallengesException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.NO_AVAILABLE_CHALLENGES)

class InvalidRecoveryLevelException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.INVALID_RECOVERY_LEVEL)

class InvalidGoalException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.INVALID_GOAL)

class GoalAnalysisFailedException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.GOAL_ANALYSIS_FAILED)

class InsufficientContextException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.INSUFFICIENT_CONTEXT)

class MongoDBConnectionException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.MONGODB_CONNECTION_ERROR)

class KafkaConnectionException(BaseAPIException):
    def __init__(self):
        super().__init__(ErrorCode.KAFKA_CONNECTION_ERROR)

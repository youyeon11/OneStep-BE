from pydantic_settings import BaseSettings
from functools import lru_cache
import os
from dotenv import load_dotenv

load_dotenv()

class Settings(BaseSettings):
    APP_ENV: str = "development"
    DEBUG: bool = True
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    
    INTERNAL_API_KEY: str = os.getenv("INTERNAL_API_KEY", "")
    
    JWT_SECRET_KEY: str = os.getenv("JWT_SECRET_KEY", "")
    
    CORS_ORIGINS: str = os.getenv("CORS_ORIGINS", "http://localhost:3000,http://localhost:8081")
    
    GMS_KEY: str = os.getenv("GMS_KEY", "")
    GMS_BASE_URL: str = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"
    GMS_LLM_MODEL: str = "gpt-4o-mini"
    GMS_EMBEDDING_MODEL: str = "text-embedding-3-small"
    
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    OPENAI_MODEL: str = "gpt-4-turbo-preview"
    OPENAI_EMBEDDING_MODEL: str = "text-embedding-3-small"
    
    CHROMA_HOST: str = os.getenv("CHROMA_HOST", "localhost")
    CHROMA_PORT: int = int(os.getenv("CHROMA_PORT", "8000"))
    CHROMA_COLLECTION_NAME: str = "challenges"
    
    MONGO_DB_NAME: str = os.getenv("MONGO_DB_NAME", "onestep_db")
    MONGO_USERNAME: str = os.getenv("MONGO_USERNAME", "admin")
    MONGO_PASSWORD: str = os.getenv("MONGO_PASSWORD", "password")
    MONGO_HOST: str = os.getenv("MONGO_HOST", "localhost")
    MONGO_PORT: int = int(os.getenv("MONGO_PORT", "27017"))
    MONGO_COLLECTION: str = os.getenv("MONGO_COLLECTION", "daily_routine_snapshots")
    
    @property
    def MONGO_URI(self) -> str:
        return f"mongodb://{self.MONGO_USERNAME}:{self.MONGO_PASSWORD}@{self.MONGO_HOST}:{self.MONGO_PORT}"
    
    KAFKA_BOOTSTRAP_SERVERS: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9094")
    KAFKA_GROUP_ID: str = os.getenv("KAFKA_GROUP_ID", "fastapi-group")
    TOPIC_TO_SPRING: str = os.getenv("TOPIC_TO_SPRING", "to-spring")
    TOPIC_FROM_SPRING: str = os.getenv("TOPIC_FROM_SPRING", "to-fastapi")
    
    SPRING_BACKEND_URL: str = os.getenv("SPRING_BACKEND_URL", "http://localhost:8080")
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "allow"

@lru_cache()
def get_settings() -> Settings:
    return Settings()

settings = get_settings()

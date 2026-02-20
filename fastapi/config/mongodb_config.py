import os
from dotenv import load_dotenv
from pydantic.v1 import BaseSettings

load_dotenv()

class MongoSettings(BaseSettings):
    MONGO_DB_NAME: str = os.getenv("MONGO_DB_NAME")
    MONGO_USERNAME: str = os.getenv("MONGO_USERNAME")
    MONGO_PASSWORD: str = os.getenv("MONGO_PASSWORD")
    MONGO_HOST: str = os.getenv("MONGO_HOST")
    MONGO_PORT: int = os.getenv("MONGO_PORT")
    MONGO_COLLECTION: str = os.getenv("MONGO_COLLECTION")

    @property
    def MONGO_URI(self) -> str:
        # mongodb://admin:password@localhost:27017 형식을 생성
        return f"mongodb://{self.MONGO_USERNAME}:{self.MONGO_PASSWORD}@{self.MONGO_HOST}:{self.MONGO_PORT}"
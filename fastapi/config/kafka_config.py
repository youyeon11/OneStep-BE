import os
from dotenv import load_dotenv
from pydantic.v1 import BaseSettings

load_dotenv()

class KafkaSettings(BaseSettings):
    KAFKA_BOOTSTRAP_SERVERS: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS")
    KAFKA_GROUP_ID: str = os.getenv("KAFKA_GROUP_ID")
    TOPIC_TO_SPRING: str = os.getenv("TOPIC_TO_SPRING")
    TOPIC_FROM_SPRING: str = os.getenv("TOPIC_FROM_SPRING")

    MONGO_URI: str = os.getenv("MONGO_URI")
    MONGO_DB_NAME: str = os.getenv("MONGO_DB_NAME")
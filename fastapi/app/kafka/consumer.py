import asyncio
import json
from aiokafka import AIOKafkaConsumer

class KafkaConsumerManager:
    def __init__(self, bootstrap_servers: str, topic: str, group_id: str):
        self.consumer = AIOKafkaConsumer(
            topic,
            bootstrap_servers=bootstrap_servers,
            group_id=group_id,
            auto_offset_reset='earliest',
            value_deserializer=lambda m: json.loads(m.decode('utf-8'))
        )

    async def run(self):
        await self.consumer.start()
        try:
            async for msg in self.consumer:
                print(f"Received message in class: {msg.value}")
        finally:
            await self.consumer.stop()
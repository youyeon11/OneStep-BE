"""
Spring -> Kafka -> FastAPI -> MongoDB 통합 플로우 테스트
- 기본 Kafka 플로우 검증
- 대규모 배치 성능 테스트
- ML 상호작용 로그 & 모델 자동 학습 테스트
"""
import asyncio
import json
import uuid
import time
import random
import sys
import os
from datetime import datetime, timedelta
from aiokafka import AIOKafkaProducer

sys.path.insert(0, "/app")

from app.repositories.interaction_repository import InteractionRepository
from app.ml.model_manager import model_manager

KAFKA_BOOTSTRAP_SERVERS = "kafka:9092"
TOPIC = "user.routine.generate"


def create_spring_message(user_code: str, with_history: bool = False) -> dict:
    """Spring이 보내는 Kafka 메시지 형식 시뮬레이션 (daily_recommendation_schemas 반영)"""
    challenge_history = []
    
    if with_history:
        for i in range(random.randint(10, 20)):
            completed = random.random() > 0.3
            challenge_history.append({
                "challengeMasterId": random.randint(1, 100),
                "assignedDate": (datetime.now() - timedelta(days=random.randint(1, 90))).strftime("%Y-%m-%d"),
                "challengeStatus": "COMPLETED" if completed else "ASSIGNED",
                "emotion": random.randint(3, 5) if completed else None,
                "origin": "RECOMMENDED"
            })
    else:
        challenge_history = [
            {
                "challengeMasterId": 1,
                "assignedDate": "2026-01-30",
                "challengeStatus": "COMPLETED",
                "emotion": 4,
                "origin": "RECOMMENDED"
            },
            {
                "challengeMasterId": 5,
                "assignedDate": "2026-01-31",
                "challengeStatus": "ASSIGNED",
                "emotion": None,
                "origin": "RECOMMENDED"
            }
        ]
    
    return {
        "id": str(uuid.uuid4()),
        "type": "ROUTINE_GENERATE_REQUEST",
        "payload": {
            "userCode": user_code,
            "requestDate": datetime.now().strftime("%Y-%m-%d"),
            "count": 5,
            "userContext": {
                "recoveryLevel": random.randint(1, 3) if with_history else 2
            },
            "challengeHistory": challenge_history,
            "userWeights": [],
            "route": None
        },
        "timestamp": int(datetime.now().timestamp() * 1000)
    }


async def send_test_message():
    """기본 플로우 테스트 (3명)"""
    producer = AIOKafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode('utf-8')
    )
    
    await producer.start()
    print(f"[Producer] Kafka 연결 완료: {KAFKA_BOOTSTRAP_SERVERS}")
    
    try:
        test_users = ["TEST_USER_001", "TEST_USER_002", "TEST_USER_003"]
        
        for user_code in test_users:
            message = create_spring_message(user_code)
            
            await producer.send_and_wait(TOPIC, message, key=user_code.encode())
            print(f"[Producer] 메시지 전송 완료: user_code={user_code}, topic={TOPIC}")
        
        print(f"\n✅ {len(test_users)}개 메시지 전송 완료!")
        
    finally:
        await producer.stop()


async def send_batch_messages(user_count: int, with_history: bool = False):
    """대규모 배치 전송"""
    print(f"\n📤 배치 전송 ({user_count}명, 히스토리={'포함' if with_history else '없음'})")
    
    producer = AIOKafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode('utf-8')
    )
    
    await producer.start()
    start_time = time.time()
    
    try:
        tasks = []
        for i in range(user_count):
            user_code = f"BATCH_USER_{i:04d}"
            message = create_spring_message(user_code, with_history=with_history)
            task = producer.send_and_wait(TOPIC, message, key=user_code.encode())
            tasks.append(task)
        
        await asyncio.gather(*tasks)
        
        send_time = time.time() - start_time
        print(f"   ✅ 전송 완료 ({send_time:.2f}초, {user_count/send_time:.1f} msg/s)")
        
    finally:
        await producer.stop()
    
    return start_time


async def check_mongodb_result():
    """MongoDB에서 추천 결과 확인"""
    from motor.motor_asyncio import AsyncIOMotorClient
    from config.settings import settings
    
    client = AsyncIOMotorClient(settings.MONGO_URI)
    db = client[settings.MONGO_DB_NAME]
    
    collection = db["daily_routine_snapshots"]
    
    print("\n[MongoDB 확인]")
    cursor = collection.find({"userCode": {"$regex": "^TEST_USER"}})
    
    count = 0
    async for doc in cursor:
        print(f"  - userCode: {doc['userCode']}")
        print(f"    recommendations: {len(doc.get('recommendations', []))}개")
        print(f"    generatedAt: {doc.get('metadata', {}).get('generatedAt')}")
        print()
        count += 1
    
    if count == 0:
        print("  ❌ 추천 결과가 없습니다. Consumer 로그를 확인하세요.")
    
    client.close()
    return count


async def check_ml_status():
    """ML 상호작용 & 모델 상태 확인"""
    from motor.motor_asyncio import AsyncIOMotorClient
    from config.settings import settings
    
    client = AsyncIOMotorClient(settings.MONGO_URI)
    db = client[settings.MONGO_DB_NAME]
    
    interaction_repo = InteractionRepository(db)
    stats = await interaction_repo.get_stats()
    models = model_manager.get_available_models()
    
    print("\n[ML 상태]")
    print(f"  📊 상호작용:")
    print(f"     - 총: {stats['total_interactions']}개")
    print(f"     - 완료: {stats['completed_interactions']}개")
    print(f"     - 유니크 유저: {stats['unique_users']}명")
    print(f"     - 유니크 챌린지: {stats['unique_challenges']}개")
    print(f"  🤖 학습된 모델: {[k for k, v in models.items() if v]}")
    
    client.close()
    return stats


async def main():
    print("=" * 70)
    print("🚀 통합 플로우 테스트 (Kafka -> FastAPI -> MongoDB -> ML)")
    print("=" * 70)
    
    # ==========================================
    # 테스트 1: 기본 플로우 (3명)
    # ==========================================
    print("\n" + "─" * 70)
    print("📊 테스트 1: 기본 플로우 (3명)")
    print("─" * 70)
    
    print("\n[1/2] Kafka 메시지 전송 중...")
    await send_test_message()
    
    print("\n⏳ FastAPI 처리 대기 (5초)...")
    await asyncio.sleep(5)
    
    print("\n[2/2] MongoDB 결과 확인 중...")
    try:
        count = await check_mongodb_result()
        if count > 0:
            print(f"✅ {count}개 추천 생성 확인")
    except Exception as e:
        print(f"  ❌ MongoDB 연결 실패: {e}")
    
    # ==========================================
    # 테스트 2: 대규모 배치 (200명, 히스토리 포함)
    # ==========================================
    print("\n" + "─" * 70)
    print("📊 테스트 2: 대규모 배치 (200명, 히스토리 포함)")
    print("─" * 70)
    
    start_time = await send_batch_messages(200, with_history=True)
    
    print("\n⏳ 처리 대기 (10초)...")
    await asyncio.sleep(10)
    
    try:
        from motor.motor_asyncio import AsyncIOMotorClient
        from config.settings import settings
        
        client = AsyncIOMotorClient(settings.MONGO_URI)
        db = client[settings.MONGO_DB_NAME]
        
        batch_count = await db["daily_routine_snapshots"].count_documents({
            "userCode": {"$regex": "^BATCH_USER_"}
        })
        
        total_time = time.time() - start_time
        print(f"\n📊 배치 처리 결과:")
        print(f"   - 처리 완료: {batch_count}/200명")
        print(f"   - 총 소요 시간: {total_time:.2f}초")
        if batch_count > 0:
            print(f"   - 처리량: {batch_count/total_time:.1f} users/s")
        
        # ML 상태 확인
        await check_ml_status()
        
        # 정리
        deleted = await db["daily_routine_snapshots"].delete_many({"userCode": {"$regex": "^BATCH_USER_"}})
        print(f"\n🧹 배치 테스트 데이터 정리: {deleted.deleted_count}개")
        
        client.close()
        
    except Exception as e:
        print(f"  ❌ 처리 실패: {e}")
    
    print("\n" + "=" * 70)
    print("🎉 테스트 완료!")
    print("=" * 70)


if __name__ == "__main__":
    asyncio.run(main())

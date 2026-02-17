#!/bin/bash
set -e

echo "======================================"
echo "FastAPI AI Server 초기화 시작"
echo "======================================"

# 1. MongoDB 연결 대기
echo "MongoDB 연결 대기 중..."
MAX_RETRIES=30
RETRY_COUNT=0

until python -c "
import asyncio
from motor.motor_asyncio import AsyncIOMotorClient
async def check():
    try:
        client = AsyncIOMotorClient('mongodb://${MONGO_USERNAME}:${MONGO_PASSWORD}@${MONGO_HOST}:27017/?authSource=admin')
        await client.admin.command('ping')
        client.close()
        return True
    except Exception as e:
        return False
result = asyncio.run(check())
exit(0 if result else 1)
" 2>/dev/null; do
  RETRY_COUNT=$((RETRY_COUNT + 1))
  if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
    echo "❌ MongoDB 연결 실패 (최대 재시도 횟수 초과)"
    exit 1
  fi
  echo "MongoDB 연결 실패, 5초 후 재시도... ($RETRY_COUNT/$MAX_RETRIES)"
  sleep 5
done
echo "✅ MongoDB 연결 성공"

# 2. ChromaDB 연결 대기
echo ""
echo "ChromaDB 연결 대기 중..."
MAX_RETRIES=30
RETRY_COUNT=0

until python -c "
import chromadb
try:
    client = chromadb.HttpClient(host='${CHROMA_HOST}', port=${CHROMA_PORT})
    client.heartbeat()
    exit(0)
except Exception as e:
    exit(1)
" 2>/dev/null; do
  RETRY_COUNT=$((RETRY_COUNT + 1))
  if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
    echo "❌ ChromaDB 연결 실패 (최대 재시도 횟수 초과)"
    exit 1
  fi
  echo "ChromaDB 연결 실패, 5초 후 재시도... ($RETRY_COUNT/$MAX_RETRIES)"
  sleep 5
done
echo "✅ ChromaDB 연결 성공"

# 3. ChromaDB 초기화 확인 및 실행
echo ""
echo "ChromaDB 데이터 확인 중..."

COLLECTION_COUNT=$(python -c "
import chromadb
try:
    client = chromadb.HttpClient(host='${CHROMA_HOST}', port=${CHROMA_PORT})
    collections = client.list_collections()
    for col in collections:
        if col.name == 'challenges':
            print(col.count())
            exit(0)
    print(0)
except Exception as e:
    print(0)
" 2>/dev/null)

if [ "$COLLECTION_COUNT" -eq 0 ] 2>/dev/null; then
    echo "⚠️  ChromaDB 데이터 없음, 초기화 시작..."
    python scripts/init_chromadb.py
    if [ $? -eq 0 ]; then
        echo "✅ ChromaDB 초기화 완료"
    else
        echo "⚠️  ChromaDB 초기화 실패, 계속 진행..."
    fi
else
    echo "✅ ChromaDB 데이터 존재 (${COLLECTION_COUNT}개), 초기화 스킵"
fi

# 4. FastAPI 서버 시작
echo ""
echo "======================================"
echo "FastAPI AI Server 시작"
echo "======================================"
exec uvicorn main:app --host 0.0.0.0 --port 8000 --reload

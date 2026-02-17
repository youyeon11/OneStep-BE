# FastAPI AI Server - OneStep Challenge Recommendation

> ML 기반 챌린지 추천 및 AI 챗봇 서버

## 📋 주요 기능

| 기능 | 호출 주체 | 인증 | 상태 |
|------|----------|------|------|
| 일일 챌린지 추천 (5개) | Spring (Kafka) | - | ✅ 완료 |
| 초기 챌린지 추천 (20개) | Spring 직접 처리 | - | ✅ 완료 |
| ML 모델 자동 학습 | 배치 트리거 | - | ✅ 완료 |
| ML 관리 API | 내부 | - | ✅ 완료 |
| AI 챗봇 (Phase 3) | 프론트엔드 | JWT | ✅ 완료 |

---

## 🎯 추천 알고리즘

### 4가지 추천 모델 (자동 선택)

| 모델 | 타입 | 활성화 조건 | 설명 |
|------|------|------------|------|
| **Rule-based** | Hybrid CBF | 항상 | 콘텐츠 기반 + 행동 가중치 |
| **LightFM** | Hybrid | 1000+ 상호작용 | 협업 필터링 + 피처 기반 |
| **ALS** | CF | 5000+ 상호작용 | 암묵적 피드백 기반 협업 필터링 |
| **2-Stage** | Ensemble | 10000+ 상호작용 | ALS (검색) + LightGBM (랭킹) |

**자동 전환 로직:**
- 총 상호작용 수 & 개별 유저 상호작용 수로 최적 모델 선택
- 500개 상호작용마다 자동 재학습
- Cold-start 유저는 항상 Rule-based 사용

### Rule-based Hybrid 알고리즘

```
최종 점수 = (1 - α) × CBF 점수 + α × 행동 가중치

α = min(완료 챌린지 수 / 50, 0.7)  # 동적 가중치

행동 가중치:
- 감정 점수 (1-5점)
- 완료 챌린지 카테고리 선호도
- 선호 난이도
- 최근 완료 챌린지 패널티 (다양성)
```

---

## 🏗️ 시스템 아키텍처

```
┌──────────────────┐
│  Spring Backend  │ (매일 23:00 스케줄러)
└────────┬─────────┘
         │ Kafka: user.routine.generate
         ▼
┌─────────────────────────────────────┐
│      FastAPI AI Server              │
│                                     │
│  • Kafka Consumer (배치 처리)       │
│  • ML 모델 자동 학습 (500개마다)    │
│  • 4가지 추천 알고리즘              │
│  • AI 챗봇 (OpenAI + LangChain)    │
│  • MongoDB 임시 저장                │
└────────┬──────────────┬─────────────┘
         │              │
         ▼              ▼
┌─────────────┐  ┌──────────────┐
│   MongoDB   │  │  ChromaDB    │
│ (데이터저장) │  │ (벡터 검색)  │
│             │  │ (서버 모드)  │
└─────────────┘  └──────────────┘
         │
         ▼
┌──────────────────┐
│  Spring Backend  │ (조회 & PostgreSQL 저장)
└──────────────────┘
```

**데이터 흐름:**
1. **일일 추천**: Spring → Kafka → FastAPI → ML 추천 → MongoDB → Spring → PostgreSQL
2. **AI 챗봇**: Android → FastAPI → OpenAI/ChromaDB → FastAPI → Android

---

## 📂 프로젝트 구조

```
fastapi/
├── main.py                          # FastAPI 앱 진입점
├── requirements.txt                 # Python 의존성
├── Dockerfile                       # Docker 빌드
├── docker-compose.local.yaml        # 로컬 개발용
├── .gitignore                       # Git 무시 (models/ 포함)
│
├── app/
│   ├── routers/
│   │   ├── health_router.py         # 헬스체크
│   │   ├── recommendation_router.py # 추천 API
│   │   ├── chat_router.py           # ✨ AI 챗봇 (Phase 3)
│   │   └── ml_router.py             # ML 관리 API
│   │
│   ├── services/
│   │   ├── daily_recommendation_service.py  # 일일 추천
│   │   ├── initial_recommendation_service.py # 초기 추천
│   │   ├── chat_service.py          # ✨ AI 챗봇 서비스
│   │   └── vector_store.py          # ✨ ChromaDB 벡터 검색
│   │
│   ├── recommenders/                # 추천 알고리즘
│   │   ├── base.py                  # BaseRecommender
│   │   ├── factory.py               # RecommenderFactory
│   │   ├── rule_based.py            # Rule-based Hybrid
│   │   ├── lightfm_recommender.py   # LightFM
│   │   ├── als_recommender.py       # ALS
│   │   └── two_stage_recommender.py # 2-Stage Pipeline
│   │
│   ├── ml/                          # ML 자동화
│   │   ├── model_manager.py         # 모델 저장/로드/선택
│   │   ├── model_trainer.py         # 모델 학습
│   │   └── scheduler.py             # 배치 트리거 학습
│   │
│   ├── repositories/
│   │   ├── challenge_repository.py
│   │   ├── user_profile_repository.py
│   │   ├── daily_recommendation_repository.py
│   │   ├── interaction_repository.py # 상호작용 로그
│   │   └── chat_repository.py       # ✨ 챗봇 세션/메시지
│   │
│   ├── schemas/
│   │   ├── common.py                # JsonResult
│   │   ├── daily_recommendation_schemas.py
│   │   ├── interaction_schemas.py   # 상호작용 스키마
│   │   └── chat_schemas.py          # ✨ 챗봇 스키마
│   │
│   ├── kafka/
│   │   ├── consumer.py              # Kafka Consumer
│   │   ├── producer.py              # Kafka Producer
│   │   └── handlers.py              # 메시지 핸들러
│   │
│   ├── exceptions/                  # 예외 처리
│   ├── dependencies/                # 의존성 주입
│   └── utils/                       # 유틸리티
│
├── config/                          # 설정
├── models/                          # 학습된 모델 (*.pkl, gitignore)
├── scripts/
│   └── init_chromadb.py             # ✨ ChromaDB 초기화
├── mongo-init/                      # MongoDB 초기 데이터
└── tests/
    ├── conftest.py
    ├── test_health.py
    ├── test_recommendation.py       # 통합 추천 테스트
    ├── test_kafka_flow.py           # E2E 플로우 테스트
    ├── model_evaluator.py           # 모델 평가
    └── data/
        └── dummy_generator.py       # 더미 데이터 생성
```

---

## 🚀 시작하기

### Docker Compose (권장)

```bash
# 1. 환경 변수 설정 (.env 파일 생성)
# GMS_KEY, MONGO_USERNAME, MONGO_PASSWORD 등 설정

# 2. 전체 인프라 실행 (FastAPI, Kafka, MongoDB, Zookeeper)
docker-compose -f docker-compose.local.yaml up -d --build

# 3. 로그 확인 (자동 초기화 확인)
docker-compose -f docker-compose.local.yaml logs -f fastapi-ai

# 4. 상태 확인
docker-compose -f docker-compose.local.yaml ps

# 5. 중지
docker-compose -f docker-compose.local.yaml down
```

**자동 초기화 프로세스:**
1. MongoDB 연결 대기 (헬스체크)
2. ChromaDB 연결 대기 (헬스체크)
3. ChromaDB 데이터 확인
   - 데이터 없음 → MongoDB에서 챌린지 200개 로드 → ChromaDB 임베딩
   - 데이터 있음 → 초기화 스킵
4. FastAPI 서버 시작

**실행 서비스:**
- FastAPI: http://localhost:8000
- API 문서: http://localhost:8000/docs
- Health Check: http://localhost:8000/health
- ChromaDB: http://localhost:8001
- MongoDB: localhost:27017
- Kafka: localhost:9094

**ChromaDB 초기화 수동 실행:**
```bash
# ChromaDB 재초기화가 필요한 경우
docker exec onestep-fastapi python scripts/init_chromadb.py

# ChromaDB 컨테이너 직접 접근
curl http://localhost:8001/api/v1/heartbeat
```

### 로컬 개발

```bash
# 1. ChromaDB, Kafka, MongoDB 실행
docker-compose -f docker-compose.local.yaml up -d chromadb kafka mongodb zookeeper

# 2. Python 가상환경
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 3. 의존성 설치
pip install -r requirements.txt

# 4. 환경 변수 설정
# .env 파일에 GMS_KEY, CHROMA_HOST=localhost, CHROMA_PORT=8001 등 설정

# 5. ChromaDB 초기화 (최초 1회)
python scripts/init_chromadb.py

# 6. 서버 실행
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

---

## 📡 API 엔드포인트

### 헬스체크

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/health` | 서버 상태 확인 |
| `GET` | `/health/ready` | 서비스 준비 상태 |

### ML 관리 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/v1/ml/status` | ML 시스템 상태 (스케줄러, 모델, 통계) |
| `POST` | `/api/v1/ml/train` | 수동 모델 학습 트리거 |
| `GET` | `/api/v1/ml/interactions/stats` | 상호작용 통계 |
| `GET` | `/api/v1/ml/models/{model_type}` | 특정 모델 정보 |

**사용 예시:**
```bash
# ML 시스템 상태 확인
curl http://localhost:8000/api/v1/ml/status

# 응답 예시
{
  "scheduler": {
    "mode": "batch_triggered",
    "is_training": false,
    "last_training_count": 6371,
    "min_threshold": 500,
    "training_interval": 500
  },
  "available_models": {
    "lightfm": true,
    "als": true,
    "two_stage": true
  },
  "interaction_stats": {
    "total_interactions": 9015,
    "completed_interactions": 6371,
    "unique_users": 403,
    "unique_challenges": 100,
    "avg_interactions_per_user": 22.37
  }
}
```

### AI 챗봇 API (Phase 3 완료)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `POST` | `/api/v1/chat/start` | 채팅 세션 시작 | JWT |
| `POST` | `/api/v1/chat/{sessionId}` | 메시지 전송 | JWT |

**AI 챗봇 기능:**
- **LangChain 기반 AI Agent**
  - OpenAI GPT-4o-mini 사용
  - Function Calling으로 도구 선택
  
- **2가지 추천 도구**:
  1. **목표 세분화** (`breakdown_goal`)
     - 큰 목표를 실천 가능한 작은 목표로 분해
     - JSON 형식으로 구조화된 목표 반환
  
  2. **챌린지 추천** (`recommend_challenge`)
     - RAG 기반 의미 검색 (ChromaDB)
     - 사용자 의도에 맞는 챌린지 추천
     - 간접적 표현, 감정 상태도 인식

**사용 예시:**
```bash
# 1. 채팅 세션 시작
curl -X POST http://localhost:8000/api/v1/chat/start \
  -H "Authorization: Bearer {JWT_TOKEN}"

# 응답: {"isSuccess": true, "result": {"sessionId": 1}}

# 2. 목표 세분화 요청
curl -X POST http://localhost:8000/api/v1/chat/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"message": "건강한 아침 루틴 만들기"}'

# 응답: analyzedGoals에 세분화된 목표 리스트

# 3. 챌린지 추천 요청
curl -X POST http://localhost:8000/api/v1/chat/1 \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"message": "운동 관련 챌린지 추천해줘"}'

# 응답: recommendations에 추천 챌린지 리스트
```

### 일일 추천 (Kafka 배치 - 내부 자동 처리)

**Kafka Topic**: `user.routine.generate`

**처리 흐름:**
1. Spring → Kafka 메시지 전송 (매일 23:00)
2. FastAPI Consumer → 추천 생성 & MongoDB 저장
3. FastAPI → 상호작용 로그 저장 (`user_interactions`)
4. FastAPI → 500개마다 모델 자동 학습
5. Spring → MongoDB 조회 → PostgreSQL 저장

---

## 🧪 테스트

### 서비스 상태 확인

```bash
# 1. Health Check
docker exec onestep-fastapi curl -s http://localhost:8000/health

# 2. ML 시스템 상태
docker exec onestep-fastapi curl -s http://localhost:8000/api/v1/ml/status

# 3. ChromaDB 데이터 확인
docker exec onestep-fastapi python -c "
from app.services.vector_store import VectorStoreService
import asyncio
vs = VectorStoreService()
count = asyncio.run(vs.get_collection_count())
print(f'ChromaDB Challenges: {count} items')
"

# 4. MongoDB 컬렉션 확인
docker exec onestep-mongodb mongosh -u onestepadmin -p onestepa508 \
  --authenticationDatabase admin --eval "
  use onestep_db
  print('Challenges:', db.challenges.countDocuments({}))
  print('User Profiles:', db.user_profiles.countDocuments({}))
  print('Chat Sessions:', db.chat_sessions.countDocuments({}))
"
```

### E2E 통합 테스트

```bash
# Docker 환경에서 전체 플로우 테스트
docker exec onestep-fastapi python tests/test_kafka_flow.py

# 테스트 내용:
# 1. 기본 플로우 (3명)
# 2. 대규모 배치 (200명, 히스토리 포함)
# 3. ML 상호작용 & 모델 자동 학습 확인
```

### 단위 테스트

```bash
# Docker 환경
docker exec onestep-fastapi pytest tests/ -v

# 로컬 환경
pytest tests/ -v
```

---

## 🛠️ 기술 스택

| 카테고리 | 기술 |
|---------|------|
| **Framework** | FastAPI 0.109.0, Python 3.10+ |
| **비동기** | asyncio, uvicorn, motor |
| **메시지 큐** | Kafka (aiokafka) |
| **데이터베이스** | MongoDB (motor) |
| **벡터DB** | ChromaDB 0.5.23 (서버 모드) |
| **AI/LLM** | OpenAI GPT-4o-mini, LangChain, Function Calling |
| **ML/추천** | LightFM, implicit (ALS), LightGBM, numpy, scipy |
| **스케줄링** | APScheduler |
| **검증** | Pydantic 2.6 |
| **로깅** | loguru |
| **테스트** | pytest, pytest-asyncio |
| **배포** | Docker (multi-stage), Docker Compose |

---

## 🔧 개발 상태

### ✅ Phase 0 - 인프라 세팅 (완료)
- FastAPI 프로젝트 구조
- MongoDB, Kafka, Zookeeper 연동
- 예외 처리 & 로깅 시스템
- Docker Compose 환경

### ✅ Phase 1 - 초기 추천 (완료)
- CBF (Content-Based Filtering)
- MongoDB 챌린지 데이터 (200개)
- 초기 추천 로직 (Spring 처리로 변경)

### ✅ Phase 2 - 일일 추천 & ML 자동화 (완료)
- **Kafka 배치 처리** (1000명/7초)
- **4가지 추천 알고리즘**:
  - Rule-based Hybrid (CBF + 행동 가중치)
  - LightFM Hybrid
  - ALS Collaborative Filtering
  - 2-Stage Pipeline (ALS + LightGBM)
- **ML 자동화 시스템**:
  - 상호작용 로그 자동 저장
  - 500개마다 모델 자동 학습
  - 모델 저장/로드/캐싱 (pickle)
  - 자동 모델 선택 (상호작용 수 기반)
- **MongoDB 저장**:
  - `daily_routine_snapshots` (일일 추천)
  - `user_profiles` (유저 프로필)
  - `user_interactions` (상호작용 로그)
- **ML 관리 API** (상태 조회, 수동 학습)

### ✅ Phase 3 - AI 챗봇 (완료)
- **OpenAI GPT-4o-mini 통합**
  - GMS API 사용 (SSAFY 제공)
  - LangChain 기반 AI Agent
  - Function Calling으로 도구 선택
- **2가지 추천 도구**:
  - 목표 세분화 (breakdown_goal)
  - 챌린지 추천 (recommend_challenge)
- **RAG 기반 대화 추천**
  - ChromaDB 벡터 검색 (200개 챌린지 임베딩)
  - 의미 기반 유사도 검색
- **MongoDB 세션 관리**:
  - `chat_sessions` (세션 정보, TTL 7일)
  - `chat_messages` (메시지 히스토리, TTL 7일)
- **동시성 제어**:
  - 세션별 Lock으로 순차 처리
  - API Rate Limiting (Semaphore)
  - Retry 메커니즘 (tenacity)

---

## 📝 MongoDB 컬렉션

| 컬렉션 | 용도 | TTL |
|--------|------|-----|
| `challenges` | 챌린지 마스터 데이터 (200개) | - |
| `daily_routine_snapshots` | 일일 추천 결과 | 7일 |
| `user_profiles` | 유저 프로필 (챗봇용) | 30일 |
| `user_interactions` | 상호작용 로그 (ML 학습용) | 90일 |
| `chat_sessions` | 챗봇 세션 정보 | 7일 |
| `chat_messages` | 챗봇 메시지 히스토리 | 7일 |

---

## 🐛 트러블슈팅

### ML 모델이 학습되지 않아요

```bash
# 1. 상호작용 수 확인
curl http://localhost:8000/api/v1/ml/status

# 2. 최소 500개 필요
# - LightFM: 1000+ (유저별 5+)
# - ALS: 5000+ (유저별 10+)
# - 2-Stage: 10000+ (유저별 20+)

# 3. 수동 학습 트리거
curl -X POST http://localhost:8000/api/v1/ml/train
```

### Kafka 메시지가 처리되지 않아요

```bash
# 1. Consumer 로그 확인
docker logs onestep-fastapi --tail 50

# 2. Kafka 토픽 확인
docker exec onestep-kafka kafka-topics --list --bootstrap-server localhost:9092

# 3. FastAPI 재시작
docker restart onestep-fastapi
```

### AI 챗봇이 응답하지 않아요

```bash
# 1. ChromaDB 데이터 확인
docker exec onestep-fastapi python -c "
from app.services.vector_store import VectorStoreService
import asyncio
vs = VectorStoreService()
count = asyncio.run(vs.get_collection_count())
print(f'ChromaDB Challenges: {count} items')
"

# 2. GMS_KEY 확인
docker exec onestep-fastapi env | grep GMS_KEY

# 3. 챗봇 로그 확인
docker logs onestep-fastapi | grep -i "chat"

# 4. ChromaDB 재초기화
docker exec onestep-fastapi python scripts/init_chromadb.py
```

---

## 📖 주요 문서

- `Phase2_일일챌린지추천_구현계획.md` - 일일 추천 설계
- `Phase3_챗봇_구현계획.md` - AI 챗봇 설계
- `# FastAPI AI Server API 명세서.md` - API 명세
- `FastAPI Convention.md` - 코드 컨벤션

---

**작성일**: 2026-01-27  
**최종 업데이트**: 2026-02-02  
**버전**: v3.0.0 (Phase 3 완료 - AI 챗봇 시스템)

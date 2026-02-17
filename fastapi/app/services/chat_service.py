import asyncio
import json
import re
from datetime import datetime
from typing import Optional, List, Dict, Any
from collections import defaultdict

from langchain_openai import ChatOpenAI
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.repositories.chat_repository import ChatRepository
from app.repositories.user_profile_repository import UserProfileRepository
from app.repositories.interaction_repository import InteractionRepository
from app.repositories.challenge_repository import ChallengeRepository
from app.services.vector_store import VectorStoreService
from config.settings import settings
from app.utils.logger import app_logger as logger


# AI Agent 기반 채팅 서비스
class ChatService:
    
    _session_locks: dict[int, asyncio.Lock] = defaultdict(asyncio.Lock)
    
    # GMS API 동시 호출 제한
    _api_semaphore = asyncio.Semaphore(10)
    
    def __init__(self, mongo_db: AsyncIOMotorDatabase):
        self.mongo_db = mongo_db
        self.chat_repo = ChatRepository(mongo_db)
        self.user_profile_repo = UserProfileRepository(mongo_db)
        self.interaction_repo = InteractionRepository(mongo_db)
        self.challenge_repo = ChallengeRepository(mongo_db)
        
        self.vector_store = VectorStoreService()
        
        # LangChain LLM 설정 (GMS API)
        self.llm = ChatOpenAI(
            model=settings.GMS_LLM_MODEL,      # gpt-4o-mini
            temperature=0.7,
            api_key=settings.GMS_KEY,
            base_url=settings.GMS_BASE_URL,    # GMS 프록시
            max_retries=3,                     # 429 에러 시 자동 재시도
            request_timeout=30                 # 타임아웃 30초
        )
    
    async def create_session(self, user_code: str, initial_context: Optional[str] = None) -> int:
        existing = await self.chat_repo.find_active_non_expired_session(user_code)
        if existing:
            session_id = existing["sessionId"]
            logger.info(f"[ChatService] 만료되지 않은 기존 세션 반환: session_id={session_id}, user={user_code}")
            return session_id
        session_id = await self.chat_repo.create_session(user_code=user_code)
        if initial_context and initial_context.strip():
            await self.chat_repo.save_message(session_id, "assistant", initial_context.strip())
        return session_id
    
    async def validate_session(self, session_id: int, user_code: str) -> bool:
        session = await self.chat_repo.get_session(session_id, user_code)
        return session is not None
    
    async def process_message(
        self,
        session_id: int,
        user_code: str,
        message: str
    ) -> Dict[str, Any]:
        async with self._session_locks[session_id]:
            await self.chat_repo.save_message(session_id, "user", message)
            
            context = await self.chat_repo.get_recent_messages(session_id, limit=10)
            personalization = await self._load_personalization(user_code)
            
            # Agent 실행 (모든 응답은 Tool Call 결과로 처리됨)
            agent_result = await self._run_agent(
                session_id=session_id,
                message=message,
                context=context,
                personalization=personalization,
                user_code=user_code
            )
            
            # 1. 데이터 추출
            final_response = agent_result.get("response", "")
            sentiment = agent_result.get("sentiment")
            interest_tags = agent_result.get("interest_tags")
            tool_used = agent_result.get("tool_used")
            recommendation = agent_result.get("recommendation")

            # 2. 데이터 저장 (세션 기록 + 프로필 즉시 반영)
            if sentiment or interest_tags:
                # (1) 세션 히스토리에 분석 결과 저장
                await self.chat_repo.update_last_user_message_analysis(
                    session_id, sentiment=sentiment, interest_tags=interest_tags
                )
                
                # (2) 프로필 즉시 업데이트
                updates = {}
                if sentiment:
                    updates["recent_mood"] = sentiment
                
                if interest_tags:
                    profile = personalization.get("profile", {}) or {}
                    existing_tags = list(profile.get("interestTags") or profile.get("interest_tags") or [])
                    merged_tags = list(dict.fromkeys(existing_tags + interest_tags))[:10]
                    updates["interest_tags"] = merged_tags
                
                if updates:
                    await self.user_profile_repo.update_from_chat(user_code, **updates)
                    logger.info(f"[ChatService] 프로필 즉시 반영: {updates}")

            # 3. 프론트 응답과 동일한 type/recommends 계산 (저장·응답 공용)
            response_type, recommends = self._build_response_type_and_recommends(
                tool_used, recommendation, final_response
            )

            # 4. Assistant 메시지 저장 (챌린지/구간 있으면 type, recommends 함께 저장)
            if final_response:
                await self.chat_repo.save_message(
                    session_id,
                    "assistant",
                    final_response,
                    response_type=response_type,
                    recommends=recommends,
                )

            logger.info(f"[ChatService] AI Agent 응답: session={session_id}, "
                        f"user={user_code}, type={response_type}")

            return {
                "type": response_type,
                "response": final_response,
                "chatSessionId": session_id,
                "recommends": recommends
            }
    
    async def _run_agent(
        self,
        session_id: int,
        message: str,
        context: List[dict],
        personalization: dict,
        user_code: str
    ) -> Dict[str, Any]:
        
        # 1위만 사용. 1위 거리 > 1.3 이면 RAG 미사용
        RAG_MAX_DISTANCE = 0.8

        # --- [1] Tool 정의: 비즈니스 로직 ---
        # [수정됨] generated_title 인자 추가
        async def recommend_challenge(keywords: list[str], user_sentiment: str, generated_title: str = "맞춤 챌린지") -> str:
            profile = personalization.get("profile", {}) or {}
            recovery_level = profile.get("recoveryLevel") or profile.get("recovery_level") or 2
            recent_ids = profile.get("recentChallengeIds") or profile.get("recent_challenge_ids") or []
            challenges_above = []

            # [수정] generated_title이 있으면 키워드 검색 스킵하고 바로 customRecommend 사용
            # LLM이 구체적인 제목을 생성했다면, 그것을 우선 사용해야 함
            # (사용자가 "다른 거 추천해줘"라고 했을 때 새로운 추천을 위해 생성한 것)
            use_generated_title = generated_title and generated_title != "맞춤 챌린지"
            
            if use_generated_title:
                # generated_title이 있으면 키워드 검색 스킵하고 바로 customRecommend로
                logger.info(f"[recommend_challenge] generated_title 사용 (키워드 검색 스킵): '{generated_title}'")
            else:
                # generated_title이 없을 때만 키워드 검색 수행
                # 1) 키워드 검색
                if keywords:
                    keyword_challenges = await self.challenge_repo.find_by_keywords(
                        keywords, recovery_level, limit=5
                    )
                    if keyword_challenges:
                        challenges_above = [
                            {
                                "challengeId": c.get("challenge_code"),
                                "title": c.get("title", ""),
                                "category": c.get("category", ""),
                                "difficultyLevel": c.get("difficulty_level", 1),
                            }
                            for c in keyword_challenges
                        ]
                        logger.info(f"[recommend_challenge] 키워드 매칭 사용: {len(challenges_above)}개")

                # 2) 키워드 매칭 없으면 벡터(RAG) 검색
                if not challenges_above:
                    query = " ".join(keywords) if keywords else ""
                    if query:
                        scored = await self.vector_store.search_challenges_with_scores(query, k=5)
                        if scored:
                            best_challenge, best_dist = scored[0]
                            if best_dist <= RAG_MAX_DISTANCE:
                                challenges_above = [best_challenge]
                                logger.info(f"[recommend_challenge] RAG 1위 사용 (거리={best_dist:.4f})")
                            else:
                                logger.info(f"[recommend_challenge] RAG 1위 거리 {best_dist:.4f} > {RAG_MAX_DISTANCE} → 탈락")

            # 필터링 및 1개만 선택
            filtered = [
                c for c in challenges_above
                if c["difficultyLevel"] <= recovery_level
                and c["challengeId"] not in recent_ids
            ][:1]

            custom_recommend = len(filtered) == 0
            
            # 로깅: 최종 선택 결과
            if custom_recommend and use_generated_title:
                logger.info(f"[recommend_challenge] 최종: customRecommend=True, generated_title='{generated_title}' 사용")
            elif filtered:
                logger.info(f"[recommend_challenge] 최종: DB 챌린지 사용 - '{filtered[0].get('title')}'")
            
            # [수정됨] generatedTitle 포함하여 리턴
            return json.dumps({
                "type": "CONVERSATION",
                "challenges": filtered,
                "keywords": keywords,
                "sentiment": user_sentiment,
                "customRecommend": custom_recommend,
                "generatedTitle": generated_title
            }, ensure_ascii=False)

        async def breakdown_goal(goal_text: str, difficulty_perception: str = "MEDIUM") -> str:
            profile = personalization.get("profile", {}) or {}
            recovery_level = profile.get("recoveryLevel") or profile.get("recovery_level") or 2
            
            breakdown_prompt = f"""
당신은 목표 달성 전문가입니다. 사용자의 목표를 실행 가능한 단계(3~4개)로 나누세요.

사용자 목표: "{goal_text}"
회복력 수준: {recovery_level}/3 (1=초기, 2=중간, 3=활발)

**아래 JSON 형식으로만 응답하세요. 다른 내용은 넣지 마세요.**

{{
  "mainGoal": "목표를 한 문장으로 정리",
  "steps": [
    {{ "step": 1, "action": "첫 번째 실행 행동" }},
    {{ "step": 2, "action": "두 번째 실행 행동" }},
    {{ "step": 3, "action": "세 번째 실행 행동" }}
  ]
}}
"""
            try:
                response = await self.llm.ainvoke(breakdown_prompt)
                response_text = response.content.strip()
                if "```json" in response_text:
                    response_text = response_text.split("```json")[1].split("```")[0].strip()
                elif "```" in response_text:
                    response_text = response_text.split("```")[1].split("```")[0].strip()
                
                breakdown = json.loads(response_text)
            except Exception as e:
                logger.error(f"[breakdown_goal] 오류: {e}")
                breakdown = {"mainGoal": goal_text, "steps": []}

            return json.dumps({
                "type": "SEGMENTATION",
                "mainGoal": breakdown.get("mainGoal", goal_text),
                "actionSteps": breakdown.get("steps", [])
            }, ensure_ascii=False)

        # --- [2] Tool 정의: 일반 대화 (신규) ---
        async def conversational_response(message_to_user: str, user_sentiment: str, keywords: list[str]) -> str:
            return json.dumps({
                "response": message_to_user,
                "sentiment": user_sentiment,
                "keywords": keywords
            }, ensure_ascii=False)

        # --- [3] Prompt & Tools 설정 ---
        system_prompt = self._build_agent_prompt(personalization)
        messages = [{"role": "system", "content": system_prompt}]
        for msg in context:
            messages.append({"role": msg["role"], "content": msg["content"]})
        messages.append({"role": "user", "content": message})
        
        # [수정됨] recommend_challenge에 generated_title 추가 및 필수 지정
        tools = [
            {
                "type": "function",
                "function": {
                    "name": "recommend_challenge",
                    "description": "사용자가 직접 수행할 수 있는 행동(미션)을 추천하거나, '아무거나 추천해줘' 같은 요청 시 사용합니다.",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "keywords": {"type": "array", "items": {"type": "string"}, "description": "관심사 키워드"},
                            "user_sentiment": {"type": "string", "enum": ["POSITIVE", "NEUTRAL", "ANXIOUS", "SAD"]},
                            "generated_title": {
                                "type": "string", 
                                "description": "DB에 적절한 챌린지가 없을 경우를 대비해, 네가 직접 창작한 구체적인 추천 행동 제목 (예: '따뜻한 차 마시기', '5분 스트레칭 하기')"
                            }
                        },
                        "required": ["keywords", "user_sentiment", "generated_title"]
                    }
                }
            },
            {
                "type": "function",
                "function": {
                    "name": "breakdown_goal",
                    "description": "사용자가 목표 달성 방법이나 구체적 계획을 물어볼 때 사용합니다.",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "goal_text": {"type": "string", "description": "사용자 목표"},
                            "difficulty_perception": {"type": "string", "enum": ["EASY", "MEDIUM", "HARD"]}
                        },
                        "required": ["goal_text"]
                    }
                }
            },
            {
                "type": "function",
                "function": {
                    "name": "conversational_response",
                    "description": "위의 두 도구(recommend_challenge, breakdown_goal)를 사용할 상황이 아닐 때, 일반적인 대화 응답을 위해 **반드시** 이 도구를 사용하세요.",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "message_to_user": {
                                "type": "string", 
                                "description": "사용자에게 보낼 답변 텍스트 (최대 3문장)"
                            },
                            "user_sentiment": {
                                "type": "string",
                                "enum": ["POSITIVE", "NEUTRAL", "ANXIOUS", "SAD"]
                            },
                            "keywords": {
                                "type": "array",
                                "items": {"type": "string"}
                            }
                        },
                        "required": ["message_to_user", "user_sentiment", "keywords"]
                    }
                }
            }
        ]
        
        # --- [4] LLM 실행 ---
        final_result = {
            "response": "",
            "sentiment": None,
            "interest_tags": None,
            "tool_used": None,
            "recommendation": None
        }

        async with self._api_semaphore:
            # tool 호출 강제
            llm_with_tools = self.llm.bind_tools(tools) 
            response = await llm_with_tools.ainvoke(messages)
            
            if hasattr(response, 'tool_calls') and response.tool_calls:
                tool_call = response.tool_calls[0]
                tool_name = tool_call['name']
                tool_args = tool_call.get('args') or {}
                
                logger.info(f"[Function Calling] Tool={tool_name}, Args={tool_args}")
                
                final_result["tool_used"] = tool_name
                
                # 공통적으로 모든 툴에서 감정과 키워드 추출
                if "user_sentiment" in tool_args:
                    final_result["sentiment"] = tool_args["user_sentiment"]
                if "keywords" in tool_args:
                    final_result["interest_tags"] = tool_args["keywords"]

                try:
                    if tool_name == "recommend_challenge":
                        # [수정됨] generated_title 전달
                        res_str = await recommend_challenge(
                            tool_args.get('keywords', []), 
                            tool_args.get('user_sentiment', 'NEUTRAL'),
                            tool_args.get('generated_title', '나만의 작은 도전')
                        )
                        recommendation = json.loads(res_str)
                        final_result["recommendation"] = recommendation
                        
                        messages.append(response) # assistant tool call
                        messages.append({
                            "role": "tool",
                            "content": res_str,
                            "tool_call_id": tool_call['id']
                        })
                        final_msg = await self.llm.ainvoke(messages)
                        final_result["response"] = final_msg.content

                    elif tool_name == "breakdown_goal":
                        res_str = await breakdown_goal(tool_args.get('goal_text', message), tool_args.get('difficulty_perception', 'MEDIUM'))
                        recommendation = json.loads(res_str)
                        final_result["recommendation"] = recommendation
                        messages.append(response)
                        messages.append({
                            "role": "tool",
                            "content": res_str,
                            "tool_call_id": tool_call['id']
                        })
                        messages.append({
                            "role": "user",
                            "content": "위 도구 결과로 단계는 이미 카드로 보여줄 거야. 사용자 요청에 맞춰 **최대 2문장**으로만 따뜻하게 답해줘. 단계를 나열하거나 반복하지 말고 간단히 격려만 해줘."
                        })
                        final_msg = await self.llm.ainvoke(messages)
                        final_result["response"] = final_msg.content

                    elif tool_name == "conversational_response":
                        final_result["response"] = tool_args.get("message_to_user", "")
                
                except Exception as e:
                    logger.error(f"[Tool Error] {e}")
                    final_result["response"] = "죄송합니다. 잠시 후 다시 시도해주세요."

            else:
                logger.warning("[ChatService] LLM이 Tool을 호출하지 않음 (Prompt 강제 실패)")
                final_result["response"] = response.content
        
        return final_result

    def _build_response_type_and_recommends(
        self,
        tool_used: Optional[str],
        recommendation: Optional[Dict[str, Any]],
        response_text: Optional[str] = None,
    ) -> tuple[str, Optional[List[Dict[str, Any]]]]:
        if tool_used == "recommend_challenge" and recommendation:
            challenges = recommendation.get("challenges") or []
            # 1. DB 챌린지 있음
            if challenges:
                recommends = [
                    {
                        "challengeCode": c.get("challengeId"),
                        "title": c.get("title", ""),
                        "origin": "RECOMMENDED",
                        "status": "ASSIGNED",
                        "reward": 20,
                    }
                    for c in challenges
                ]
                return "recommend", recommends
            
            # 2. 맞춤(Custom) 챌린지 (DB 없음)
            # [수정됨] generatedTitle을 우선 사용하여 제목 불일치 해결
            if recommendation.get("customRecommend"):
                ai_generated_title = recommendation.get("generatedTitle")
                
                # Fallback: 혹시나 비어있을 경우 기존 로직
                if ai_generated_title:
                    title = ai_generated_title
                else:
                    keywords = recommendation.get("keywords")
                    if keywords and "아무거나" not in keywords:
                        title = f"{keywords[0]} 하기"
                    else:
                        title = "나를 위한 작은 도전"

                recommends = [
                    {
                        "challengeCode": None,
                        "title": title,  # AI가 만든 구체적 제목 들어감
                        "origin": "SELF",
                        "status": "ASSIGNED",
                        "reward": 0,
                    }
                ]
                return "recommend", recommends
            
            return "recommend", None
            
        if tool_used == "breakdown_goal" and recommendation:
            steps = recommendation.get("actionSteps") or []
            recommends = [
                {
                    "challengeCode": None,
                    "title": step.get("action", ""),
                    "origin": "SELF",
                    "status": "ASSIGNED",
                    "reward": 0,
                }
                for step in steps[:4]
            ]
            return "segment", recommends
        return "common", None

    async def _sync_session_profile_to_user(self, session_id: int, user_code: str) -> None:
        messages = await self.chat_repo.get_all_messages_for_session(session_id)
        sentiments = [m.get("sentiment") for m in messages if m.get("sentiment")]
        all_tags = []
        for m in messages:
            tags = m.get("interestTags") or []
            if isinstance(tags, list):
                all_tags.extend(tags)
        recent_mood = sentiments[-1] if sentiments else None
        merged_tags = list(dict.fromkeys(all_tags))[:10]
        if recent_mood or merged_tags:
            await self.user_profile_repo.update_from_chat(
                user_code,
                recent_mood=recent_mood,
                interest_tags=merged_tags if merged_tags else None,
            )
        logger.info(f"[ChatService] 세션 종료 프로필 동기화: session_id={session_id}, user={user_code}")

    async def close_session(self, session_id: int, user_code: str) -> None:
        session = await self.chat_repo.get_session(session_id, user_code)
        if not session:
            return
        await self._sync_session_profile_to_user(session_id, user_code)
        await self.chat_repo.close_session(session_id)
        logger.info(f"[ChatService] 세션 종료: session_id={session_id}, user={user_code}")

    # 개인화 컨텍스트 로드
    async def _load_personalization(self, user_code: str) -> dict:
        profile = await self.user_profile_repo.find_by_user_code(user_code)
        interactions = await self.interaction_repo.get_user_interactions(user_code)
        
        recent_emotions = [
            i.get("emotion") for i in interactions[:5] 
            if i.get("emotion") is not None
        ]
        emotion_trend = self._calc_emotion_trend(recent_emotions)
        success_rate = self._calc_success_rate(interactions)
        
        return {
            "profile": profile or {},
            "recent_challenges": interactions[:5],
            "emotion_trend": emotion_trend,
            "success_rate": success_rate
        }
    
    def _calc_emotion_trend(self, emotions: List[int]) -> str:
        if len(emotions) < 2:
            return "안정"
        avg_recent = sum(emotions[:3]) / min(len(emotions), 3)
        avg_old = sum(emotions[3:]) / max(len(emotions) - 3, 1) if len(emotions) > 3 else avg_recent
        if avg_recent > avg_old + 0.5:
            return "상승"
        elif avg_recent < avg_old - 0.5:
            return "하락"
        return "안정"
    
    def _calc_success_rate(self, interactions: List[dict]) -> float:
        if not interactions:
            return 0.0
        completed = sum(1 for i in interactions if i.get("interactionType") == "COMPLETED")
        return round(completed / len(interactions) * 100, 1)
    
    def _build_agent_prompt(self, personalization: dict) -> str:
        profile = personalization.get("profile", {}) or {}
        pet_name = profile.get("petNickname") or profile.get("pet_nickname") or "토리"
        recovery_level = profile.get("recoveryLevel") or profile.get("recovery_level") or 2
        interest_tags = profile.get("interestTags") or profile.get("interest_tags") or []
        interest_display = ", ".join(interest_tags[:3]) if interest_tags else "아직 없음 (채팅을 통해 파악 예정)"
        completed_count = profile.get("completedChallengeCount") or profile.get("completed_challenge_count") or 0
        emotion_trend = personalization.get("emotion_trend", "안정")
        
        return f"""당신은 OneStep 서비스의 강아지 펫 친구 '{pet_name}'입니다.
사회적 고립을 경험하는 사용자들의 회복을 돕는 따뜻하고 공감적인 친구입니다.

## {pet_name}의 성격 (Persona)
- 격려와 공감을 아끼지 않는 다정한 친구입니다.
- 가르치려 들지 않고, 사용자의 속도에 맞춰 함께 걷습니다.
- ~해, ~할까?, ~했구나!, ~하자 처럼 부드럽고 친근한 구어체를 사용합니다.
- 텍스트를 칠 때 장황하게 쓰지 않고, 카톡 하듯이 짧고 간결하게 툭툭 던지는 스타일입니다.

## 사용자 정보
- 회복력 수준: {recovery_level} (1=초기/위축, 2=중간/회복중, 3=활발/유지)
- 관심 태그: {interest_display}
- 완료 챌린지: {completed_count}개
- 감정 추이: {emotion_trend}

## 응답 가이드 (Strict Rules)
- **[필수] 길이 제한**: 답변은 **최대 3문장**으로 끝내세요. (절대 3문장을 넘기지 마세요.)
- **[필수] 간결성**: 모바일 채팅 환경이므로 내용이 길면 말풍선이 잘립니다. 핵심만 짧게 말하세요.
- **번호 매기기 금지**: 1., 2. 같은 리스트나 개조식 서술을 절대 금지합니다.
- **공감 우선**: 짧더라도 첫 마디는 반드시 따뜻한 공감으로 시작하세요.
- **질문 제한**: 질문은 한 번에 딱 하나만 하세요.

## Tool 사용 지침 (매우 중요)
사용자의 발화에 따라 **반드시** 다음 세 가지 도구 중 하나를 호출해야 합니다. 절대로 도구 호출 없이 일반 텍스트로만 응답하지 마세요.

1. **recommend_challenge**: 
   - 사용자가 '무언가 직접 행동하고 싶어 하거나', '아무거나 추천해줘' 같이 행동을 제안받길 원할 때 사용하세요.
   - **중요**: DB에 적절한 챌린지가 없을 경우를 대비해, **네가 생각한 구체적인 추천 활동 제목을 `generated_title` 파라미터에 반드시 넣어주세요.**
     (예: "따뜻한 차 마시기", "5분 스트레칭 하기", "창문 열고 환기하기")
   - 주의: 단순히 "맛집 추천해줘", "뭐 먹을까?", "영화 추천해줘" 같이 **정보나 의견을 묻는 경우에는 절대 사용하지 말고** conversational_response를 사용하세요.
   
2. **breakdown_goal**: 
   - 구체적인 목표 달성 방법이나 루틴 계획을 물을 때

3. **conversational_response**: 
   - 위 두 경우가 아닌 **모든 일반적인 대화** (인사, 위로, 잡담, 단순 답변 등)에서 사용하세요.
   - `message_to_user` 인자에 사용자에게 보낼 따뜻한 답변을 적으세요.

모든 Tool 호출 시, `user_sentiment`와 `keywords`를 분석하여 인자로 전달하세요.
"""
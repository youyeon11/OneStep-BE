from datetime import datetime, timedelta, timezone
from typing import Optional, List, Dict, Any
from motor.motor_asyncio import AsyncIOMotorDatabase
from pymongo import ReturnDocument

from app.utils.logger import app_logger as logger

# 한국 시간대 (KST, UTC+9)
KST = timezone(timedelta(hours=9))


# MongoDB에서 채팅 세션 및 메시지 CRUD
class ChatRepository:
    
    COUNTER_COLLECTION = "counters"
    SESSION_COLLECTION = "chat_sessions"
    MESSAGE_COLLECTION = "chat_messages"
    
    def __init__(self, db: AsyncIOMotorDatabase):
        self.db = db
    
    async def _get_next_session_id(self) -> int:
        result = await self.db[self.COUNTER_COLLECTION].find_one_and_update(
            {"_id": "chat_session_id"},
            {"$inc": {"seq": 1}},
            upsert=True,
            return_document=ReturnDocument.AFTER
        )
        return result["seq"]
    
    async def create_session(self, user_code: str) -> int:
        session_id = await self._get_next_session_id()
        
        now = datetime.now(KST)
        session_doc = {
            "sessionId": session_id,
            "userCode": user_code,
            "status": "active",
            "createdAt": now,
            "updatedAt": now,
            "expiresAt": now + timedelta(hours=6)
        }
        
        await self.db[self.SESSION_COLLECTION].insert_one(session_doc)
        
        logger.info(f"[ChatRepository] 세션 생성: session_id={session_id}, user={user_code}")
        return session_id
    
    async def get_session(self, session_id: int, user_code: str) -> Optional[Dict[str, Any]]:
        session = await self.db[self.SESSION_COLLECTION].find_one({
            "sessionId": session_id,
            "userCode": user_code,
            "status": "active"
        })
        return session

    async def get_session_by_id(self, session_id: int) -> Optional[Dict[str, Any]]:
        return await self.db[self.SESSION_COLLECTION].find_one({"sessionId": session_id})

    async def find_active_non_expired_session(self, user_code: str) -> Optional[Dict[str, Any]]:
        now = datetime.now(KST)
        session = await self.db[self.SESSION_COLLECTION].find_one(
            {
                "userCode": user_code,
                "status": "active",
                "expiresAt": {"$gt": now}
            },
            sort=[("updatedAt", -1)]
        )
        return session
    
    async def save_message(
        self,
        session_id: int,
        role: str,
        content: str,
        *,
        sentiment: Optional[str] = None,
        interest_tags: Optional[List[str]] = None,
        response_type: Optional[str] = None,
        recommends: Optional[List[Dict[str, Any]]] = None,
    ) -> None:
        message_doc = {
            "sessionId": session_id,
            "role": role,
            "content": content,
            "createdAt": datetime.now(KST)
        }
        if sentiment is not None:
            message_doc["sentiment"] = sentiment
        if interest_tags is not None:
            message_doc["interestTags"] = interest_tags
        if response_type is not None:
            message_doc["type"] = response_type
        if recommends is not None:
            message_doc["recommends"] = recommends

        await self.db[self.MESSAGE_COLLECTION].insert_one(message_doc)
        
        await self.db[self.SESSION_COLLECTION].update_one(
            {"sessionId": session_id},
            {"$set": {"updatedAt": datetime.now(KST)}}
        )
    
    async def update_last_user_message_analysis(
        self,
        session_id: int,
        sentiment: Optional[str] = None,
        interest_tags: Optional[List[str]] = None,
    ) -> bool:
        if sentiment is None and interest_tags is None:
            return False
        update = {}
        if sentiment is not None:
            update["sentiment"] = sentiment
        if interest_tags is not None:
            update["interestTags"] = interest_tags
        last_user = await self.db[self.MESSAGE_COLLECTION].find_one(
            {"sessionId": session_id, "role": "user"},
            sort=[("createdAt", -1)]
        )
        if not last_user:
            return False
        result = await self.db[self.MESSAGE_COLLECTION].update_one(
            {"_id": last_user["_id"]},
            {"$set": update}
        )
        return result.matched_count > 0
    
    async def get_recent_messages(self, session_id: int, limit: int = 10) -> List[Dict[str, Any]]:
        cursor = self.db[self.MESSAGE_COLLECTION].find(
            {"sessionId": session_id}
        ).sort("createdAt", -1).limit(limit)
        
        messages = await cursor.to_list(length=limit)
        
        return list(reversed(messages))
    
    async def get_all_messages_for_session(self, session_id: int) -> List[Dict[str, Any]]:
        cursor = self.db[self.MESSAGE_COLLECTION].find(
            {"sessionId": session_id}
        ).sort("createdAt", 1)
        return await cursor.to_list(length=500)
    
    async def count_user_messages(self, session_id: int) -> int:
        return await self.db[self.MESSAGE_COLLECTION].count_documents(
            {"sessionId": session_id, "role": "user"}
        )
    
    async def update_session_sync_count(self, session_id: int, count: int) -> None:
        await self.db[self.SESSION_COLLECTION].update_one(
            {"sessionId": session_id},
            {"$set": {"lastSyncedMessageCount": count, "updatedAt": datetime.now(KST)}}
        )
    
    async def close_session(self, session_id: int) -> None:
        await self.db[self.SESSION_COLLECTION].update_one(
            {"sessionId": session_id},
            {"$set": {
                "status": "closed",
                "updatedAt": datetime.now(KST)
            }}
        )
        
        logger.info(f"[ChatRepository] 세션 종료: session_id={session_id}")

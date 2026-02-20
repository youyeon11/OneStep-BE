from threading import Lock
from typing import List, Dict, Any, Tuple
import chromadb
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma

from config.settings import settings
from app.utils.logger import app_logger as logger

# ChromaDB 기반 챌린지 검색 서비스 (서버 모드)
class VectorStoreService:
    
    _instance = None
    _lock = Lock()
    
    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialize()
        return cls._instance
    
    def _initialize(self):
        logger.info("[VectorStore] 초기화 시작")
        
        self.embeddings = OpenAIEmbeddings(
            model=settings.GMS_EMBEDDING_MODEL,
            api_key=settings.GMS_KEY,
            base_url=settings.GMS_BASE_URL,
            max_retries=3
        )
        
        # ChromaDB HTTP 클라이언트 (서버 모드)
        self.chroma_client = chromadb.HttpClient(
            host=settings.CHROMA_HOST,
            port=settings.CHROMA_PORT
        )
        
        # LangChain Chroma 래퍼
        self.vectorstore = Chroma(
            client=self.chroma_client,
            collection_name=settings.CHROMA_COLLECTION_NAME,
            embedding_function=self.embeddings
        )
        
        logger.info(
            f"[VectorStore] 초기화 완료: "
            f"host={settings.CHROMA_HOST}:{settings.CHROMA_PORT}, "
            f"collection={settings.CHROMA_COLLECTION_NAME}"
        )
    
    async def add_challenges(self, challenges: List[Dict[str, Any]]) -> None:
        if not challenges:
            logger.warning("[VectorStore] 저장할 챌린지가 없습니다")
            return
        
        texts = []
        metadatas = []
        ids = []
        
        for c in challenges:
            tags_list = c.get("tags", [])
            tags_str = ", ".join(tags_list) if tags_list else ""
            text = (
                f"카테고리: {c.get('category', '')} | "
                f"제목: {c.get('title', '')} | "
                f"설명: {c.get('description', '')} | "
                f"태그: {tags_str}"
            )
            texts.append(text)
            
            metadatas.append({
                "challengeId": str(c.get("challengeId")),
                "title": c.get("title", ""),
                "category": c.get("category", ""),
                "difficultyLevel": c.get("difficultyLevel", 1)
            })
            
            ids.append(f"challenge_{c.get('challengeId')}")
        
        # ChromaDB에 텍스트와 메타데이터 저장
        self.vectorstore.add_texts(
            texts=texts,
            metadatas=metadatas,
            ids=ids
        )
        
        logger.info(f"[VectorStore] {len(challenges)}개 챌린지 저장 완료 (임베딩 텍스트 구조 개선됨)")
    
    # 챌린지 검색(RAG) - 유사도(거리) 포함. 거리 낮을수록 유사.
    async def search_challenges_with_scores(
        self,
        query: str,
        k: int = 5,
        filter_dict: Dict[str, Any] = None
    ) -> List[Tuple[Dict[str, Any], float]]:
        try:
            results = self.vectorstore.similarity_search_with_score(
                query=query,
                k=k,
                filter=filter_dict
            )
            out = []
            for doc, score in results:
                out.append(({
                    "challengeId": int(doc.metadata.get("challengeId", 0)),
                    "title": doc.metadata.get("title", ""),
                    "category": doc.metadata.get("category", ""),
                    "difficultyLevel": doc.metadata.get("difficultyLevel", 1),
                }, float(score)))
            
            distances = [round(s, 4) for _, s in out]
            logger.info(f"[VectorStore] 검색 완료: query='{query}', results={len(out)}, distances={distances}")
            return out
        except Exception as e:
            logger.error(f"[VectorStore] 검색 실패: {e}")
            return []

    async def search_challenges(
        self,
        query: str,
        k: int = 5,
        filter_dict: Dict[str, Any] = None
    ) -> List[Dict[str, Any]]:
        results = await self.search_challenges_with_scores(query, k, filter_dict)
        return [c for c, _ in results]
    
    async def get_collection_count(self) -> int:
        try:
            collection = self.vectorstore._collection
            return collection.count()
        except Exception as e:
            logger.error(f"[VectorStore] 카운트 조회 실패: {e}")
            return 0
from fastapi import APIRouter, Depends, HTTPException, Query
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.schemas.common import JsonResult
from app.dependencies.auth import verify_internal_api_key
from app.dependencies.mongodb import get_mongo_db

COLLECTION_DAILY_RECOMMENDATIONS = "daily_recommendations"

router = APIRouter(
    prefix="/api/v1/recommendations",
    tags=["Recommendations"],
    dependencies=[Depends(verify_internal_api_key)],
)


@router.get("/documents", response_model=JsonResult)
async def list_recommendation_documents(
    user_code: int | None = Query(None, gt=0),
    from_date: str | None = Query(None),
    to_date: str | None = Query(None),
    limit: int = Query(50, ge=1, le=200),
    mongo_db: AsyncIOMotorDatabase = Depends(get_mongo_db),
) -> JsonResult:
    """일일 추천 문서 목록 조회 (MongoDB)."""
    coll = mongo_db[COLLECTION_DAILY_RECOMMENDATIONS]
    q = {}
    if user_code is not None:
        q["user_code"] = user_code
    if from_date or to_date:
        q["date"] = {}
        if from_date:
            q["date"]["$gte"] = from_date
        if to_date:
            q["date"]["$lte"] = to_date
    cursor = coll.find(q).sort("date", -1).limit(limit)
    items = await cursor.to_list(length=limit)
    for doc in items:
        if "_id" in doc:
            doc["_id"] = str(doc["_id"])
    return JsonResult.success(result={"items": items, "count": len(items)})


@router.get("/documents/{document_id}", response_model=JsonResult)
async def get_recommendation_document(
    document_id: str,
    mongo_db: AsyncIOMotorDatabase = Depends(get_mongo_db),
) -> JsonResult:
    """일일 추천 문서 단건 조회 (MongoDB _id)."""
    from bson import ObjectId

    coll = mongo_db[COLLECTION_DAILY_RECOMMENDATIONS]
    try:
        oid = ObjectId(document_id)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid document_id")
    doc = await coll.find_one({"_id": oid})
    if doc is None:
        raise HTTPException(status_code=404, detail="Document not found")
    doc["_id"] = str(doc["_id"])
    return JsonResult.success(result=doc)

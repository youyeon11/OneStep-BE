import numpy as np
from typing import List, Dict, Any, Set
from dataclasses import dataclass, field

from app.recommenders.base import RecommendationResult


@dataclass
class EvaluationResult:
    model_name: str
    precision_at_k: Dict[int, float] = field(default_factory=dict)
    recall_at_k: Dict[int, float] = field(default_factory=dict)
    ndcg_at_k: Dict[int, float] = field(default_factory=dict)
    hit_rate_at_k: Dict[int, float] = field(default_factory=dict)
    coverage: float = 0.0
    diversity: float = 0.0
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "model_name": self.model_name,
            "precision@k": self.precision_at_k,
            "recall@k": self.recall_at_k,
            "ndcg@k": self.ndcg_at_k,
            "hit_rate@k": self.hit_rate_at_k,
            "coverage": self.coverage,
            "diversity": self.diversity
        }


class ModelEvaluator:
    """
    추천 모델 성능 평가
    
    주요 지표:
    - Precision@K: 추천 중 관련 아이템 비율
    - Recall@K: 전체 관련 아이템 중 추천된 비율
    - NDCG@K: 순위 품질 (상위 추천일수록 가중치)
    - Hit Rate@K: 적어도 하나 맞춘 유저 비율
    - Coverage: 추천된 고유 아이템 비율
    - Diversity: 추천 내 카테고리 다양성
    """
    
    def __init__(self, k_values: List[int] = None):
        self.k_values = k_values or [1, 3, 5, 10]
    
    def evaluate(
        self,
        predictions: Dict[str, List[RecommendationResult]],
        ground_truth: Dict[str, Set[int]],
        all_items: List[Dict] = None,
        model_name: str = "unknown"
    ) -> EvaluationResult:
        result = EvaluationResult(model_name=model_name)
        
        for k in self.k_values:
            precisions = []
            recalls = []
            ndcgs = []
            hits = []
            
            for user_code, recs in predictions.items():
                if user_code not in ground_truth:
                    continue
                
                relevant = ground_truth[user_code]
                if not relevant:
                    continue
                
                predicted_ids = [r.challenge_id for r in recs[:k]]
                
                hits_count = len(set(predicted_ids) & relevant)
                precision = hits_count / k if k > 0 else 0
                precisions.append(precision)
                
                recall = hits_count / len(relevant) if relevant else 0
                recalls.append(recall)
                
                ndcg = self._calculate_ndcg(predicted_ids, relevant, k)
                ndcgs.append(ndcg)
                
                hit = 1 if hits_count > 0 else 0
                hits.append(hit)
            
            if precisions:
                result.precision_at_k[k] = round(np.mean(precisions), 4)
                result.recall_at_k[k] = round(np.mean(recalls), 4)
                result.ndcg_at_k[k] = round(np.mean(ndcgs), 4)
                result.hit_rate_at_k[k] = round(np.mean(hits), 4)
        
        if all_items:
            all_recommended = set()
            for recs in predictions.values():
                all_recommended.update(r.challenge_id for r in recs)
            all_item_ids = {item.get("challenge_code") or item.get("challenge_master_id") for item in all_items}
            all_item_ids = {x for x in all_item_ids if x is not None}
            result.coverage = round(len(all_recommended) / len(all_item_ids), 4) if all_item_ids else 0
        
        if all_items:
            result.diversity = self._calculate_diversity(predictions, all_items)
        
        return result
    
    def _calculate_ndcg(
        self,
        predicted: List[int],
        relevant: Set[int],
        k: int
    ) -> float:
        dcg = 0.0
        for i, item_id in enumerate(predicted[:k]):
            if item_id in relevant:
                dcg += 1.0 / np.log2(i + 2)
        
        ideal_hits = min(len(relevant), k)
        idcg = sum(1.0 / np.log2(i + 2) for i in range(ideal_hits))
        
        return dcg / idcg if idcg > 0 else 0.0
    
    def _calculate_diversity(
        self,
        predictions: Dict[str, List[RecommendationResult]],
        all_items: List[Dict]
    ) -> float:
        def _item_code(item):
            return item.get("challenge_code") or item.get("challenge_master_id") or item.get("challenge_id")
        item_categories = {
            _item_code(item): item.get("category", "unknown")
            for item in all_items
            if _item_code(item) is not None
        }
        
        diversities = []
        for recs in predictions.values():
            if not recs:
                continue
            categories = [item_categories.get(r.challenge_id, "unknown") for r in recs]
            unique_ratio = len(set(categories)) / len(categories) if categories else 0
            diversities.append(unique_ratio)
        
        return round(np.mean(diversities), 4) if diversities else 0.0
    
    def compare_models(
        self,
        results: List[EvaluationResult]
    ) -> Dict[str, Any]:
        comparison = {
            "models": [r.model_name for r in results],
            "metrics": {}
        }
        
        for k in self.k_values:
            comparison["metrics"][f"precision@{k}"] = {
                r.model_name: r.precision_at_k.get(k, 0) for r in results
            }
            comparison["metrics"][f"ndcg@{k}"] = {
                r.model_name: r.ndcg_at_k.get(k, 0) for r in results
            }
            comparison["metrics"][f"hit_rate@{k}"] = {
                r.model_name: r.hit_rate_at_k.get(k, 0) for r in results
            }
        
        comparison["metrics"]["coverage"] = {
            r.model_name: r.coverage for r in results
        }
        comparison["metrics"]["diversity"] = {
            r.model_name: r.diversity for r in results
        }
        
        best_models = {}
        for metric, values in comparison["metrics"].items():
            best_model = max(values, key=values.get)
            best_models[metric] = {"model": best_model, "score": values[best_model]}
        
        comparison["best_models"] = best_models
        
        return comparison
    
    def generate_report(
        self,
        results: List[EvaluationResult]
    ) -> str:
        comparison = self.compare_models(results)
        
        lines = [
            "=" * 60,
            "📊 추천 모델 성능 비교 리포트",
            "=" * 60,
            "",
            f"평가 모델: {', '.join(comparison['models'])}",
            f"평가 K값: {self.k_values}",
            "",
            "-" * 60,
            "📈 성능 지표 비교",
            "-" * 60,
        ]
        
        for k in self.k_values:
            lines.append(f"\n### K = {k}")
            lines.append(f"{'Model':<20} {'Precision':<12} {'NDCG':<12} {'Hit Rate':<12}")
            lines.append("-" * 56)
            
            for result in results:
                p = result.precision_at_k.get(k, 0)
                n = result.ndcg_at_k.get(k, 0)
                h = result.hit_rate_at_k.get(k, 0)
                lines.append(f"{result.model_name:<20} {p:<12.4f} {n:<12.4f} {h:<12.4f}")
        
        lines.extend([
            "",
            "-" * 60,
            "🎯 Coverage & Diversity",
            "-" * 60,
            f"{'Model':<20} {'Coverage':<12} {'Diversity':<12}",
            "-" * 44,
        ])
        
        for result in results:
            lines.append(f"{result.model_name:<20} {result.coverage:<12.4f} {result.diversity:<12.4f}")
        
        lines.extend([
            "",
            "-" * 60,
            "🏆 최고 성능 모델",
            "-" * 60,
        ])
        
        for metric, info in comparison["best_models"].items():
            lines.append(f"{metric}: {info['model']} ({info['score']:.4f})")
        
        lines.append("=" * 60)
        
        return "\n".join(lines)


async def run_model_comparison(num_users: int = 1000, num_challenges: int = 200):
    """4가지 추천 모델 성능 비교 실험 실행"""
    from tests.data.dummy_generator import DummyDataGenerator
    from app.recommenders.factory import RecommenderFactory, ModelType
    
    print("=" * 60)
    print("🚀 추천 모델 성능 비교 실험 시작")
    print("=" * 60)
    
    print("\n📦 데이터 생성 중...")
    generator = DummyDataGenerator(num_users=num_users, num_challenges=num_challenges)
    dataset = generator.generate_full_dataset(interactions_per_user=20)
    
    interaction_split_idx = int(len(dataset.interactions) * 0.8)
    train_interactions = dataset.interactions[:interaction_split_idx]
    test_interactions = dataset.interactions[interaction_split_idx:]
    
    print(f"  - 유저: {len(dataset.users)}")
    print(f"  - 챌린지: {len(dataset.challenges)}")
    print(f"  - 학습 인터랙션: {len(train_interactions)}")
    print(f"  - 테스트 인터랙션: {len(test_interactions)}")
    
    train_data_obj = type('obj', (object,), {
        'users': dataset.users,
        'challenges': dataset.challenges,
        'interactions': train_interactions
    })()
    
    lightfm_data = generator.prepare_lightfm_data(train_data_obj)
    
    ground_truth: Dict[str, Set[int]] = {}
    for interaction in test_interactions:
        if interaction.get("challenge_status") == "COMPLETED":
            user_code = interaction["user_code"]
            if user_code not in ground_truth:
                ground_truth[user_code] = set()
            cid = interaction.get("challenge_master_id")
            if cid is not None:
                ground_truth[user_code].add(cid)
    
    print(f"  - Ground Truth 유저: {len(ground_truth)}")
    
    print("\n🎓 모델 학습 중...")
    RecommenderFactory.clear_cache()
    
    models = {
        ModelType.RULE_BASED: "Rule-based",
        ModelType.LIGHTFM: "LightFM",
        ModelType.ALS: "ALS",
        ModelType.TWO_STAGE: "2-Stage"
    }
    
    for model_type, name in models.items():
        print(f"  - {name} 학습...")
        recommender = RecommenderFactory.get_recommender(model_type)
        
        if model_type == ModelType.RULE_BASED:
            pass
        elif model_type == ModelType.LIGHTFM:
            await recommender.fit(
                interactions=lightfm_data["interactions"],
                user_features_data=lightfm_data["user_features"],
                item_features_data=lightfm_data["item_features"],
                epochs=15
            )
        elif model_type in [ModelType.ALS, ModelType.TWO_STAGE]:
            await recommender.fit(
                interactions=lightfm_data["interactions"]
            )
    
    print("\n🎯 추천 생성 중...")
    all_predictions: Dict[str, Dict[str, List[RecommendationResult]]] = {}
    
    for model_type, name in models.items():
        print(f"  - {name} 추천 생성...")
        recommender = RecommenderFactory.get_recommender(model_type)
        predictions: Dict[str, List[RecommendationResult]] = {}
        
        evaluated_users = list(ground_truth.keys())[:50]
        
        for user_code in evaluated_users:
            user = next((u for u in dataset.users if u["user_code"] == user_code), None)
            if not user:
                continue
            
            user_features = {
                "user_code": user["user_code"],
                "preferred_categories": user["survey_tags"],
                "interest_tags": user["interest_tags"],
                "recovery_level": user["recovery_level"]
            }
            
            user_history = [
                i for i in train_interactions
                if i["user_code"] == user_code
            ]
            
            results = await recommender.recommend(
                user_features=user_features,
                item_pool=dataset.challenges,
                history=user_history,
                top_k=10
            )
            
            predictions[user["user_code"]] = results
        
        all_predictions[name] = predictions
    
    print("\n📊 성능 평가 중...")
    evaluator = ModelEvaluator(k_values=[1, 3, 5, 10])
    
    evaluation_results: List[EvaluationResult] = []
    
    for model_type, name in models.items():
        result = evaluator.evaluate(
            predictions=all_predictions[name],
            ground_truth=ground_truth,
            all_items=dataset.challenges,
            model_name=name
        )
        evaluation_results.append(result)
        print(f"  - {name}: Precision@5={result.precision_at_k.get(5, 0):.4f}")
    
    print("\n" + evaluator.generate_report(evaluation_results))
    
    return {
        "evaluation_results": [r.to_dict() for r in evaluation_results],
        "comparison": evaluator.compare_models(evaluation_results)
    }


if __name__ == "__main__":
    import asyncio
    asyncio.run(run_model_comparison())

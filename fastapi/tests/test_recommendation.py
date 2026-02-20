import pytest

from tests.data.dummy_generator import DummyDataGenerator, create_sample_dataset

class TestDummyGenerator:
    """더미 데이터 생성기 테스트"""
    
    def test_generate_users(self):
        """유저 데이터 생성 테스트"""
        generator = DummyDataGenerator(num_users=10, num_challenges=20)
        users = generator.generate_users()
        
        assert len(users) == 10
        for user in users:
            assert "user_code" in user
            assert "recovery_level" in user
            assert 1 <= user["recovery_level"] <= 3
            assert "survey_tags" in user
            assert "interest_tags" in user
    
    def test_generate_challenges(self):
        """챌린지 데이터 생성 테스트"""
        generator = DummyDataGenerator(num_users=10, num_challenges=20)
        challenges = generator.generate_challenges()
        
        assert len(challenges) == 20
        for challenge in challenges:
            assert "challenge_code" in challenge
            assert "category" in challenge
            assert challenge["category"] in ["LIFESTYLE", "SOCIAL", "INNER"]
            assert "tags" in challenge
            assert "difficulty_level" in challenge
    
    def test_generate_interactions(self):
        """상호작용 데이터 생성 테스트"""
        generator = DummyDataGenerator(num_users=10, num_challenges=20)
        users = generator.generate_users()
        challenges = generator.generate_challenges()
        interactions = generator.generate_interactions(users, challenges, 5)
        
        assert len(interactions) > 0
        for interaction in interactions:
            assert "user_code" in interaction
            assert "challenge_master_id" in interaction
            assert "challenge_status" in interaction
            assert interaction["challenge_status"] in ["COMPLETED", "ASSIGNED"]
    
    def test_generate_full_dataset(self):
        """전체 데이터셋 생성 테스트"""
        generator = DummyDataGenerator(num_users=50, num_challenges=30)
        dataset = generator.generate_full_dataset(interactions_per_user=10)
        
        assert len(dataset.users) == 50
        assert len(dataset.challenges) == 30
        assert len(dataset.interactions) > 0
        assert "num_users" in dataset.metadata
        assert "completion_rate" in dataset.metadata
    
    def test_prepare_lightfm_data(self):
        """LightFM 데이터 준비 테스트"""
        generator = DummyDataGenerator(num_users=20, num_challenges=15)
        dataset = generator.generate_full_dataset(interactions_per_user=8)
        lightfm_data = generator.prepare_lightfm_data(dataset)
        
        assert "interactions" in lightfm_data
        assert "user_features" in lightfm_data
        assert "item_features" in lightfm_data
        assert "all_user_features" in lightfm_data
        assert "all_item_features" in lightfm_data
        
        # interactions는 완료된 것만 포함
        for user_code, challenge_id, rating in lightfm_data["interactions"]:
            assert rating is not None
            assert 1 <= rating <= 5
    
    def test_train_test_split(self):
        """학습/테스트 분리 테스트"""
        generator = DummyDataGenerator(num_users=100, num_challenges=50)
        train, test = generator.generate_train_test_split(test_ratio=0.2)
        
        assert len(train.users) == 80
        assert len(test.users) == 20
        assert train.metadata["split"] == "train"
        assert test.metadata["split"] == "test"


# ============================================
# Rule-based Recommender Tests
# ============================================

class TestRuleBasedRecommender:
    """Rule-based Recommender 테스트"""
    
    @pytest.fixture
    def sample_dataset(self):
        """샘플 데이터셋"""
        return create_sample_dataset()
    
    @pytest.mark.asyncio
    async def test_rule_based_recommend(self, sample_dataset):
        """Rule-based 추천 생성 테스트"""
        from app.recommenders.rule_based import RuleBasedRecommender
        
        recommender = RuleBasedRecommender()
        
        user = sample_dataset.users[0]
        user_features = {
            "user_code": user["user_code"],
            "preferred_categories": user["survey_tags"],
            "interest_tags": user["interest_tags"],
            "recovery_level": user["recovery_level"]
        }
        
        results = await recommender.recommend(
            user_features=user_features,
            item_pool=sample_dataset.challenges,
            history=[],
            top_k=5
        )
        
        assert len(results) == 5
        for result in results:
            assert hasattr(result, "challenge_id")
            assert hasattr(result, "weight")
            assert 0 <= result.weight <= 1
    
    @pytest.mark.asyncio
    async def test_rule_based_with_history(self, sample_dataset):
        """Rule-based 히스토리 포함 테스트"""
        from app.recommenders.rule_based import RuleBasedRecommender
        
        recommender = RuleBasedRecommender()
        
        user = sample_dataset.users[0]
        user_features = {
            "user_code": user["user_code"],
            "preferred_categories": user["survey_tags"],
            "interest_tags": user["interest_tags"],
            "recovery_level": user["recovery_level"]
        }
        
        # 히스토리 추가
        user_history = [
            i for i in sample_dataset.interactions
            if i["user_code"] == user["user_code"]
        ][:5]
        
        results = await recommender.recommend(
            user_features=user_features,
            item_pool=sample_dataset.challenges,
            history=user_history,
            top_k=5
        )
        
        assert len(results) == 5
        # 히스토리 반영 확인: debug_info에 behavior 점수 존재
        for result in results:
            assert "debug_info" in result.__dict__


class TestLightFMRecommender:
    """LightFM Recommender 테스트"""
    
    @pytest.fixture
    def sample_dataset(self):
        """샘플 데이터셋"""
        return create_sample_dataset()
    
    @pytest.fixture
    def lightfm_data(self, sample_dataset):
        """LightFM 학습 데이터"""
        generator = DummyDataGenerator()
        return generator.prepare_lightfm_data(sample_dataset)
    
    @pytest.mark.asyncio
    async def test_lightfm_import(self):
        """LightFM import 테스트"""
        from app.recommenders.lightfm_recommender import LightFMRecommender
        
        recommender = LightFMRecommender()
        info = recommender.get_model_info()
        
        assert info["model_type"] == "LightFMRecommender"
        assert "is_fitted" in info
        assert info["is_fitted"] == False
    
    @pytest.mark.asyncio
    async def test_lightfm_fit(self, lightfm_data):
        """LightFM 학습 테스트"""
        from app.recommenders.lightfm_recommender import LightFMRecommender
        
        recommender = LightFMRecommender()
        
        await recommender.fit(
            interactions=lightfm_data["interactions"],
            user_features_data=lightfm_data["user_features"],
            item_features_data=lightfm_data["item_features"],
            epochs=5
        )
        
        assert recommender.is_fitted == True
        assert recommender.model is not None
    
    @pytest.mark.asyncio
    async def test_lightfm_recommend(self, sample_dataset, lightfm_data):
        """LightFM 추천 생성 테스트"""
        from app.recommenders.lightfm_recommender import LightFMRecommender
        
        recommender = LightFMRecommender()
        
        await recommender.fit(
            interactions=lightfm_data["interactions"],
            user_features_data=lightfm_data["user_features"],
            item_features_data=lightfm_data["item_features"],
            epochs=5
        )
        
        # 기존 유저로 추천
        user = sample_dataset.users[0]
        user_features = {
            "user_code": user["user_code"],
            "preferred_categories": user["survey_tags"],
            "interest_tags": user["interest_tags"],
            "recovery_level": user["recovery_level"]
        }
        
        results = await recommender.recommend(
            user_features=user_features,
            item_pool=sample_dataset.challenges,
            history=[],
            top_k=5
        )
        
        assert len(results) == 5
        for result in results:
            assert hasattr(result, "challenge_id")
            assert hasattr(result, "weight")
            assert 0 <= result.weight <= 1
    
    @pytest.mark.asyncio
    async def test_lightfm_cold_start_fallback(self, sample_dataset, lightfm_data):
        """Cold Start 유저 Rule-based Fallback 테스트"""
        from app.recommenders.lightfm_recommender import LightFMRecommender
        
        recommender = LightFMRecommender()
        
        await recommender.fit(
            interactions=lightfm_data["interactions"],
            user_features_data=lightfm_data["user_features"],
            item_features_data=lightfm_data["item_features"],
            epochs=5
        )
        
        # 새로운 유저 (학습 데이터에 없음) → Rule-based Fallback
        new_user_features = {
            "user_code": "NEW_USER_999",
            "preferred_categories": ["LIFESTYLE", "INNER"],
            "interest_tags": ["운동", "명상", "산책"],
            "recovery_level": 2
        }
        
        results = await recommender.recommend(
            user_features=new_user_features,
            item_pool=sample_dataset.challenges,
            history=[],
            top_k=5
        )
        
        assert len(results) == 5
        # Cold start는 Rule-based Fallback
        for result in results:
            assert "debug_info" in result.__dict__
            # Rule-based의 debug_info 확인
            assert "cbf" in result.debug_info or "behavior" in result.debug_info
    
    @pytest.mark.asyncio
    async def test_lightfm_fallback_when_not_fitted(self):
        """학습되지 않은 상태에서 Rule-based Fallback 테스트"""
        from app.recommenders.lightfm_recommender import LightFMRecommender
        
        recommender = LightFMRecommender()
        
        user_features = {
            "user_code": "TEST_USER",
            "preferred_categories": ["LIFESTYLE"],
            "interest_tags": ["운동"],
            "recovery_level": 2
        }
        
        item_pool = [
            {"challenge_code": 1, "category": "LIFESTYLE", "tags": ["운동"], "difficulty_level": 2},
            {"challenge_code": 2, "category": "INNER", "tags": ["명상"], "difficulty_level": 1},
        ]
        
        # 학습되지 않았으므로 Rule-based fallback
        results = await recommender.recommend(
            user_features=user_features,
            item_pool=item_pool,
            history=[],
            top_k=2
        )
        
        assert len(results) == 2


class TestModelComparison:
    """모델 간 비교 테스트"""
    
    @pytest.mark.asyncio
    async def test_rule_based_vs_lightfm(self):
        """Rule-based vs LightFM 추천 결과 비교"""
        from app.recommenders.rule_based import RuleBasedRecommender
        from app.recommenders.lightfm_recommender import LightFMRecommender
        
        # 데이터 준비
        generator = DummyDataGenerator(num_users=100, num_challenges=50)
        dataset = generator.generate_full_dataset(interactions_per_user=15)
        lightfm_data = generator.prepare_lightfm_data(dataset)
        
        # 모델 초기화
        rule_based = RuleBasedRecommender()
        lightfm = LightFMRecommender()
        
        # LightFM 학습
        await lightfm.fit(
            interactions=lightfm_data["interactions"],
            user_features_data=lightfm_data["user_features"],
            item_features_data=lightfm_data["item_features"],
            epochs=10
        )
        
        # 비교 테스트
        user = dataset.users[0]
        user_features = {
            "user_code": user["user_code"],
            "preferred_categories": user["survey_tags"],
            "interest_tags": user["interest_tags"],
            "recovery_level": user["recovery_level"]
        }
        
        user_history = [
            i for i in dataset.interactions
            if i["user_code"] == user["user_code"]
        ]
        
        rule_based_results = await rule_based.recommend(
            user_features=user_features,
            item_pool=dataset.challenges,
            history=user_history,
            top_k=5
        )
        
        lightfm_results = await lightfm.recommend(
            user_features=user_features,
            item_pool=dataset.challenges,
            history=user_history,
            top_k=5
        )
        
        # 둘 다 5개 추천 생성
        assert len(rule_based_results) == 5
        assert len(lightfm_results) == 5
        
        # 결과 형식 동일
        for rb, lf in zip(rule_based_results, lightfm_results):
            assert hasattr(rb, "challenge_id")
            assert hasattr(lf, "challenge_id")
            assert hasattr(rb, "weight")
            assert hasattr(lf, "weight")
        
        # 추천 결과 비교 (다를 수 있음)
        rb_ids = {r.challenge_id for r in rule_based_results}
        lf_ids = {r.challenge_id for r in lightfm_results}
        overlap = len(rb_ids & lf_ids)
        
        print(f"\n=== 모델 비교 결과 ===")
        print(f"Rule-based: {rb_ids}")
        print(f"LightFM: {lf_ids}")
        print(f"겹치는 추천: {overlap}/5")


class TestRecommenderFactory:
    """Factory Pattern 테스트"""
    
    def test_factory_rule_based_creation(self):
        """Factory에서 Rule-based 생성 테스트"""
        from app.recommenders.factory import RecommenderFactory, ModelType
        
        RecommenderFactory.clear_cache()
        
        recommender = RecommenderFactory.get_recommender(ModelType.RULE_BASED)
        
        assert recommender is not None
        info = recommender.get_model_info()
        assert info["model_type"] == "RuleBasedRecommender"
    
    def test_factory_lightfm_creation(self):
        """Factory에서 LightFM 생성 테스트"""
        from app.recommenders.factory import RecommenderFactory, ModelType
        
        RecommenderFactory.clear_cache()
        
        recommender = RecommenderFactory.get_recommender(ModelType.LIGHTFM)
        
        assert recommender is not None
        info = recommender.get_model_info()
        assert info["model_type"] == "LightFMRecommender"
    
    def test_factory_caching(self):
        """Factory 캐싱 테스트"""
        from app.recommenders.factory import RecommenderFactory, ModelType
        
        RecommenderFactory.clear_cache()
        
        recommender1 = RecommenderFactory.get_recommender(ModelType.LIGHTFM)
        recommender2 = RecommenderFactory.get_recommender(ModelType.LIGHTFM)
        
        # 같은 인스턴스 (싱글톤)
        assert recommender1 is recommender2
    
    def test_factory_different_models(self):
        """Factory 다른 모델 생성 테스트"""
        from app.recommenders.factory import RecommenderFactory, ModelType
        
        RecommenderFactory.clear_cache()
        
        rule_based = RecommenderFactory.get_recommender(ModelType.RULE_BASED)
        lightfm = RecommenderFactory.get_recommender(ModelType.LIGHTFM)
        
        # 다른 인스턴스
        assert rule_based is not lightfm
        assert rule_based.get_model_info()["model_type"] == "RuleBasedRecommender"
        assert lightfm.get_model_info()["model_type"] == "LightFMRecommender"
    
    @pytest.mark.asyncio
    async def test_factory_runtime_switching(self):
        """Factory 런타임 모델 전환 테스트"""
        from app.recommenders.factory import RecommenderFactory, ModelType
        
        RecommenderFactory.clear_cache()
        
        dataset = create_sample_dataset()
        user = dataset.users[0]
        user_features = {
            "user_code": user["user_code"],
            "preferred_categories": user["survey_tags"],
            "interest_tags": user["interest_tags"],
            "recovery_level": user["recovery_level"]
        }
        
        # Rule-based로 추천
        rule_based = RecommenderFactory.get_recommender(ModelType.RULE_BASED)
        rb_results = await rule_based.recommend(
            user_features=user_features,
            item_pool=dataset.challenges,
            history=[],
            top_k=5
        )
        
        # LightFM으로 전환 (학습 안 됨 → Fallback)
        lightfm = RecommenderFactory.get_recommender(ModelType.LIGHTFM)
        lf_results = await lightfm.recommend(
            user_features=user_features,
            item_pool=dataset.challenges,
            history=[],
            top_k=5
        )
        
        # 둘 다 정상 작동
        assert len(rb_results) == 5
        assert len(lf_results) == 5


class TestALSRecommender:
    """ALS Recommender 테스트"""
    
    @pytest.fixture
    def sample_dataset(self):
        """샘플 데이터셋"""
        return create_sample_dataset()
    
    @pytest.fixture
    def lightfm_data(self, sample_dataset):
        """학습 데이터"""
        generator = DummyDataGenerator()
        return generator.prepare_lightfm_data(sample_dataset)
    
    @pytest.mark.asyncio
    async def test_als_import(self):
        """ALS import 테스트"""
        from app.recommenders.als_recommender import ALSRecommender
        recommender = ALSRecommender()
        assert recommender is not None
    
    @pytest.mark.asyncio
    async def test_als_fit(self, sample_dataset, lightfm_data):
        """ALS 학습 테스트"""
        from app.recommenders.als_recommender import ALSRecommender
        
        recommender = ALSRecommender()
        
        await recommender.fit(
            interactions=lightfm_data["interactions"]
        )
        
        assert recommender.is_fitted is True
        assert len(recommender.user_mapping) > 0
        assert len(recommender.item_mapping) > 0
    
    @pytest.mark.asyncio
    async def test_als_recommend(self, sample_dataset, lightfm_data):
        """ALS 추천 생성 테스트"""
        from app.recommenders.als_recommender import ALSRecommender
        
        recommender = ALSRecommender()
        
        await recommender.fit(
            interactions=lightfm_data["interactions"]
        )
        
        # 학습 데이터에 포함된 유저 사용
        trained_user_code = lightfm_data["interactions"][0][0]
        trained_user = next(
            (u for u in sample_dataset.users if u["user_code"] == trained_user_code),
            sample_dataset.users[0]
        )
        
        user_features = {
            "user_code": trained_user["user_code"],
            "preferred_categories": trained_user["survey_tags"],
            "interest_tags": trained_user["interest_tags"],
            "recovery_level": trained_user["recovery_level"]
        }
        
        user_history = [
            i for i in sample_dataset.interactions
            if i["user_code"] == trained_user["user_code"]
        ]
        
        results = await recommender.recommend(
            user_features=user_features,
            item_pool=sample_dataset.challenges,
            history=user_history,
            top_k=5
        )
        
        assert len(results) <= 5
        for r in results:
            assert hasattr(r, "challenge_id")
            assert hasattr(r, "weight")
    
    @pytest.mark.asyncio
    async def test_als_cold_start_fallback(self):
        """ALS Cold Start 유저 Fallback 테스트"""
        from app.recommenders.als_recommender import ALSRecommender
        
        recommender = ALSRecommender()
        
        # 최소 학습 데이터
        interactions = [
            ("USER_001", 1, 1.0),
            ("USER_001", 2, 1.0),
            ("USER_002", 1, 1.0),
        ]
        
        await recommender.fit(interactions=interactions)
        
        # Cold Start 유저 (학습 데이터에 없는 유저)
        user_features = {
            "user_code": "NEW_USER",
            "survey_tags": ["LIFESTYLE"],
            "interest_tags": ["운동"],
            "recovery_level": 2
        }
        
        item_pool = [
            {"challenge_id": 1, "category": "LIFESTYLE", "tags": ["운동"], "difficulty_level": 2},
            {"challenge_id": 2, "category": "INNER", "tags": ["명상"], "difficulty_level": 1},
        ]
        
        results = await recommender.recommend(
            user_features=user_features,
            item_pool=item_pool,
            history=[],
            top_k=2
        )
        
        # Rule-based Fallback 사용
        assert len(results) == 2


class TestTwoStageRecommender:
    """2-Stage Recommender 테스트"""
    
    @pytest.fixture
    def sample_dataset(self):
        """샘플 데이터셋"""
        return create_sample_dataset()
    
    @pytest.fixture
    def lightfm_data(self, sample_dataset):
        """학습 데이터"""
        generator = DummyDataGenerator()
        return generator.prepare_lightfm_data(sample_dataset)
    
    @pytest.mark.asyncio
    async def test_two_stage_import(self):
        """2-Stage import 테스트"""
        from app.recommenders.two_stage_recommender import TwoStageRecommender
        recommender = TwoStageRecommender()
        assert recommender is not None
    
    @pytest.mark.asyncio
    async def test_two_stage_fit_als_only(self, sample_dataset, lightfm_data):
        """2-Stage ALS 학습 테스트 (LightGBM 없이)"""
        from app.recommenders.two_stage_recommender import TwoStageRecommender
        
        recommender = TwoStageRecommender()
        
        # ALS만 학습 (ranking_features 없이)
        await recommender.fit(
            interactions=lightfm_data["interactions"]
        )
        
        assert recommender.is_fitted is True
        assert len(recommender.user_mapping) > 0
        assert recommender.ranking_model is None
    
    @pytest.mark.asyncio
    async def test_two_stage_fit_full(self, sample_dataset, lightfm_data):
        """2-Stage 전체 학습 테스트 (ALS + LightGBM)"""
        from app.recommenders.two_stage_recommender import TwoStageRecommender
        
        recommender = TwoStageRecommender()
        
        # 랭킹 피처 생성 (완료된 인터랙션만 사용)
        ranking_features = []
        ranking_labels = []
        
        completed = [i for i in sample_dataset.interactions if i.get("challenge_status") == "COMPLETED"]
        for interaction in completed[:500]:
            emotion = interaction.get("emotion") or 3
            ranking_features.append({
                "als_score": 0.5,
                "difficulty": 2,
            "recovery_level": 2,
                "category_match": 1.0,
                "tag_overlap": 0.5,
                "was_completed": 1.0,
                "avg_emotion": emotion
            })
            ranking_labels.append(float(emotion) / 5.0)
        
        await recommender.fit(
            interactions=lightfm_data["interactions"],
            ranking_features=ranking_features,
            ranking_labels=ranking_labels
        )
        
        assert recommender.is_fitted is True
        assert recommender.ranking_model is not None
    
    @pytest.mark.asyncio
    async def test_two_stage_recommend(self, sample_dataset, lightfm_data):
        """2-Stage 추천 생성 테스트"""
        from app.recommenders.two_stage_recommender import TwoStageRecommender
        
        recommender = TwoStageRecommender()
        
        await recommender.fit(
            interactions=lightfm_data["interactions"]
        )
        
        # 학습 데이터에 포함된 유저 사용
        trained_user_code = lightfm_data["interactions"][0][0]
        trained_user = next(
            (u for u in sample_dataset.users if u["user_code"] == trained_user_code),
            sample_dataset.users[0]
        )
        
        user_features = {
            "user_code": trained_user["user_code"],
            "preferred_categories": trained_user["survey_tags"],
            "interest_tags": trained_user["interest_tags"],
            "recovery_level": trained_user["recovery_level"]
        }
        
        user_history = [
            i for i in sample_dataset.interactions
            if i["user_code"] == trained_user["user_code"]
        ]
        
        results = await recommender.recommend(
            user_features=user_features,
            item_pool=sample_dataset.challenges,
            history=user_history,
            top_k=5
        )
        
        assert len(results) <= 5
        for r in results:
            assert hasattr(r, "challenge_id")
            assert hasattr(r, "weight")
            assert r.debug_info.get("model") == "two_stage"
    
    @pytest.mark.asyncio
    async def test_two_stage_cold_start_fallback(self):
        """2-Stage Cold Start Fallback 테스트"""
        from app.recommenders.two_stage_recommender import TwoStageRecommender
        
        recommender = TwoStageRecommender()
        
        # 미학습 상태에서 호출
        user_features = {
            "user_code": "NEW_USER",
            "preferred_categories": ["LIFESTYLE"],
            "interest_tags": ["운동"],
            "recovery_level": 2
        }
        
        item_pool = [
            {"challenge_code": 1, "category": "LIFESTYLE", "tags": ["운동"], "difficulty_level": 2},
            {"challenge_code": 2, "category": "INNER", "tags": ["명상"], "difficulty_level": 1},
        ]
        
        results = await recommender.recommend(
            user_features=user_features,
            item_pool=item_pool,
            history=[],
            top_k=2
        )
        
        # Rule-based Fallback 사용
        assert len(results) == 2
    
    @pytest.mark.asyncio
    async def test_two_stage_model_info(self):
        """2-Stage 모델 정보 테스트"""
        from app.recommenders.two_stage_recommender import TwoStageRecommender
        
        recommender = TwoStageRecommender()
        
        info = recommender.get_model_info()
        
        assert info["model_type"] == "TwoStageRecommender"
        assert "als_factors" in info["config"]
        assert "retrieval_k" in info["config"]
        assert "has_ranking_model" in info["stats"]


class TestFactoryWithAllModels:
    """Factory Pattern 전체 모델 테스트"""
    
    def test_factory_als_creation(self):
        """Factory에서 ALS 생성 테스트"""
        from app.recommenders.factory import RecommenderFactory, ModelType
        
        RecommenderFactory.clear_cache()
        
        recommender = RecommenderFactory.get_recommender(ModelType.ALS)
        
        assert recommender is not None
        info = recommender.get_model_info()
        assert info["model_type"] == "ALSRecommender"
    
    def test_factory_two_stage_creation(self):
        """Factory에서 2-Stage 생성 테스트"""
        from app.recommenders.factory import RecommenderFactory, ModelType
        
        RecommenderFactory.clear_cache()
        
        recommender = RecommenderFactory.get_recommender(ModelType.TWO_STAGE)
        
        assert recommender is not None
        info = recommender.get_model_info()
        assert info["model_type"] == "TwoStageRecommender"
    
    def test_factory_all_models(self):
        """Factory에서 모든 모델 생성 테스트"""
        from app.recommenders.factory import RecommenderFactory, ModelType
        
        RecommenderFactory.clear_cache()
        
        for model_type in ModelType:
            recommender = RecommenderFactory.get_recommender(model_type)
            assert recommender is not None
            info = recommender.get_model_info()
            assert "model_type" in info


class TestModelEvaluator:
    """ModelEvaluator 테스트"""
    
    def test_evaluator_import(self):
        """Evaluator import 테스트"""
        from tests.model_evaluator import ModelEvaluator, EvaluationResult
        
        evaluator = ModelEvaluator()
        assert evaluator is not None
        assert evaluator.k_values == [1, 3, 5, 10]
    
    def test_precision_calculation(self):
        """Precision@K 계산 테스트"""
        from tests.model_evaluator import ModelEvaluator
        from app.recommenders.base import RecommendationResult
        
        evaluator = ModelEvaluator(k_values=[3, 5])
        
        # Mock 추천 결과
        predictions = {
            "USER_001": [
                RecommendationResult(challenge_id=1, weight=0.9),
                RecommendationResult(challenge_id=2, weight=0.8),
                RecommendationResult(challenge_id=3, weight=0.7),
                RecommendationResult(challenge_id=4, weight=0.6),
                RecommendationResult(challenge_id=5, weight=0.5),
            ]
        }
        
        # Ground Truth: 1, 3은 관련 아이템
        ground_truth = {
            "USER_001": {1, 3, 10}
        }
        
        result = evaluator.evaluate(
            predictions=predictions,
            ground_truth=ground_truth,
            model_name="test"
        )
        
        # Precision@3 = 2/3 (1, 3 맞음)
        assert result.precision_at_k[3] == round(2/3, 4)
        
        # Precision@5 = 2/5
        assert result.precision_at_k[5] == round(2/5, 4)
    
    def test_ndcg_calculation(self):
        """NDCG@K 계산 테스트"""
        from tests.model_evaluator import ModelEvaluator
        from app.recommenders.base import RecommendationResult
        import numpy as np
        
        evaluator = ModelEvaluator(k_values=[3])
        
        # 1번이 첫 번째 → DCG = 1/log2(2)
        predictions = {
            "USER_001": [
                RecommendationResult(challenge_id=1, weight=0.9),
                RecommendationResult(challenge_id=2, weight=0.8),
                RecommendationResult(challenge_id=3, weight=0.7),
            ]
        }
        
        ground_truth = {"USER_001": {1}}
        
        result = evaluator.evaluate(
            predictions=predictions,
            ground_truth=ground_truth,
            model_name="test"
        )
        
        # NDCG@3: DCG = 1/log2(2) = 1, IDCG = 1/log2(2) = 1
        assert result.ndcg_at_k[3] == 1.0
    
    def test_compare_models(self):
        """모델 비교 테스트"""
        from tests.model_evaluator import ModelEvaluator, EvaluationResult
        
        evaluator = ModelEvaluator(k_values=[5])
        
        result1 = EvaluationResult(
            model_name="Model_A",
            precision_at_k={5: 0.4},
            ndcg_at_k={5: 0.5},
            hit_rate_at_k={5: 0.8},
            coverage=0.3,
            diversity=0.6
        )
        
        result2 = EvaluationResult(
            model_name="Model_B",
            precision_at_k={5: 0.6},
            ndcg_at_k={5: 0.7},
            hit_rate_at_k={5: 0.9},
            coverage=0.5,
            diversity=0.4
        )
        
        comparison = evaluator.compare_models([result1, result2])
        
        assert "models" in comparison
        assert "best_models" in comparison
        assert comparison["best_models"]["precision@5"]["model"] == "Model_B"
        assert comparison["best_models"]["diversity"]["model"] == "Model_A"
    
    def test_generate_report(self):
        """리포트 생성 테스트"""
        from tests.model_evaluator import ModelEvaluator, EvaluationResult
        
        evaluator = ModelEvaluator(k_values=[5])
        
        result = EvaluationResult(
            model_name="TestModel",
            precision_at_k={5: 0.4},
            ndcg_at_k={5: 0.5},
            hit_rate_at_k={5: 0.8},
            coverage=0.3,
            diversity=0.6
        )
        
        report = evaluator.generate_report([result])
        
        assert "추천 모델 성능 비교 리포트" in report
        assert "TestModel" in report
        assert "Precision" in report


class TestFullModelComparison:
    """전체 모델 비교 테스트"""
    
    @pytest.fixture
    def sample_dataset(self):
        """샘플 데이터셋"""
        return create_sample_dataset()
    
    @pytest.mark.asyncio
    async def test_all_models_evaluation(self, sample_dataset):
        """4가지 모델 평가 테스트"""
        from tests.model_evaluator import ModelEvaluator
        from app.recommenders.factory import RecommenderFactory, ModelType
        
        RecommenderFactory.clear_cache()
        
        generator = DummyDataGenerator()
        lightfm_data = generator.prepare_lightfm_data(sample_dataset)
        
        # Ground Truth 준비
        ground_truth = {}
        for interaction in sample_dataset.interactions:
            if interaction.get("challenge_status") == "COMPLETED":
                user_code = interaction["user_code"]
                if user_code not in ground_truth:
                    ground_truth[user_code] = set()
                ground_truth[user_code].add(interaction["challenge_master_id"])
        
        evaluator = ModelEvaluator(k_values=[5])
        results = []
        
        # 각 모델 평가
        for model_type in ModelType:
            recommender = RecommenderFactory.get_recommender(model_type)
            
            # 학습
            if model_type == ModelType.LIGHTFM:
                await recommender.fit(
                    interactions=lightfm_data["interactions"],
                    user_features_data=lightfm_data["user_features"],
                    item_features_data=lightfm_data["item_features"],
                    epochs=5
                )
            elif model_type in [ModelType.ALS, ModelType.TWO_STAGE]:
                await recommender.fit(
                    interactions=lightfm_data["interactions"]
                )
            
            # 추천 생성
            predictions = {}
            for user in sample_dataset.users[:20]:
                user_features = {
                    "user_code": user["user_code"],
                    "preferred_categories": user["survey_tags"],
                    "interest_tags": user["interest_tags"],
                    "recovery_level": user["recovery_level"]
                }
                
                recs = await recommender.recommend(
                    user_features=user_features,
                    item_pool=sample_dataset.challenges,
                    history=[],
                    top_k=5
                )
                predictions[user["user_code"]] = recs
            
            # 평가
            result = evaluator.evaluate(
                predictions=predictions,
                ground_truth=ground_truth,
                all_items=sample_dataset.challenges,
                model_name=model_type.value
            )
            results.append(result)
        
        # 4개 모델 평가 완료
        assert len(results) == 4
        
        # 비교 결과
        comparison = evaluator.compare_models(results)
        assert len(comparison["models"]) == 4
        assert "best_models" in comparison
        
        # 리포트 생성
        report = evaluator.generate_report(results)
        assert "rule_based" in report or "Rule" in report
        
        print("\n" + report)

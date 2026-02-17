import random
from typing import List, Dict, Tuple, Set
from datetime import datetime, timedelta
from dataclasses import dataclass, field


CATEGORIES = ["LIFESTYLE", "SOCIAL", "INNER"]
TAGS = [
    "운동", "명상", "산책", "독서", "음악", "요리", 
    "정리", "감사", "호흡", "스트레칭", "취미", "휴식",
    "자기개발", "소통", "봉사", "일기", "아침루틴", "저녁루틴"
]


@dataclass
class DummyUser:
    user_code: str
    recovery_level: int
    survey_tags: List[str]
    interest_tags: List[str]


@dataclass
class DummyChallenge:
    challenge_code: int
    category: str
    tags: List[str]
    difficulty_level: int


@dataclass
class DummyInteraction:
    user_code: str
    challenge_master_id: int
    challenge_status: str  # COMPLETED / ASSIGNED
    emotion: int  # 1~5
    origin: str  # RECOMMENDED / SELF
    assigned_date: str
    completed_at: str = None


@dataclass
class DummyDataset:
    users: List[Dict]
    challenges: List[Dict]
    interactions: List[Dict]
    metadata: Dict = field(default_factory=dict)


class DummyDataGenerator:
    """
    LightFM, LightGBM 학습용 더미 데이터 생성
    
    특징:
    - 현실적인 패턴 반영: 선호 카테고리에 높은 emotion
    - 유저별 선호도 다양화
    - Cold Start 시뮬레이션 (신규 유저)
    """
    
    def __init__(self, num_users: int = 500, num_challenges: int = 100, seed: int = 42):
        self.num_users = num_users
        self.num_challenges = num_challenges
        random.seed(seed)
    
    def generate_users(self) -> List[Dict]:
        """유저 데이터 생성"""
        users = []
        for i in range(self.num_users):
            survey_tags = random.sample(CATEGORIES, k=random.randint(1, 3))
            interest_tags = random.sample(TAGS, k=random.randint(3, 6))
            
            users.append({
                "user_code": f"USER_{i:05d}",
                "recovery_level": random.randint(1, 3),
                "survey_tags": survey_tags,
                "interest_tags": interest_tags
            })
        return users
    
    def generate_challenges(self) -> List[Dict]:
        """챌린지 데이터 생성"""
        challenges = []
        for i in range(self.num_challenges):
            category = random.choice(CATEGORIES)
            related_tags = self._get_category_related_tags(category)
            tags = random.sample(related_tags + TAGS, k=random.randint(2, 4))
            
            challenges.append({
                "challenge_code": i + 1,
                "category": category,
                "tags": list(set(tags)),
                "difficulty_level": random.randint(1, 3)
            })
        return challenges
    
    def _get_category_related_tags(self, category: str) -> List[str]:
        """카테고리별 관련 태그 반환"""
        tag_map = {
            "LIFESTYLE": ["운동", "산책", "스트레칭", "아침루틴", "저녁루틴", "정리"],
            "SOCIAL": ["소통", "봉사", "취미"],
            "INNER": ["명상", "호흡", "감사", "일기", "휴식", "독서"]
        }
        return tag_map.get(category, [])
    
    def generate_interactions(
        self, 
        users: List[Dict], 
        challenges: List[Dict],
        interactions_per_user: int = 20
    ) -> List[Dict]:
        """
        현실적인 패턴 반영 상호작용 생성
        
        패턴:
        - 선호 카테고리 챌린지: 높은 emotion (3.5~5.0)
        - 비선호 카테고리: 낮은 emotion (2.0~3.5)
        - 일부 유저는 ASSIGNED만 (미완료)
        """
        interactions = []
        challenge_map = {c["challenge_code"]: c for c in challenges}
        
        for user in users:
            preferred_categories = set(user["survey_tags"])
            preferred_tags = set(user["interest_tags"])
            
            num_interactions = random.randint(
                max(1, interactions_per_user - 10),
                interactions_per_user + 10
            )
            
            user_challenges = random.sample(
                challenges, 
                k=min(num_interactions, len(challenges))
            )
            
            for idx, c in enumerate(user_challenges):
                assigned_date = datetime.now() - timedelta(days=random.randint(1, 90))
                
                # 완료 확률: 선호 카테고리일수록 높음
                is_preferred = c["category"] in preferred_categories
                complete_prob = 0.8 if is_preferred else 0.5
                is_completed = random.random() < complete_prob
                
                if is_completed:
                    # emotion 결정: 선호도 + 태그 일치도 반영
                    tag_overlap = len(set(c["tags"]) & preferred_tags)
                    
                    if is_preferred and tag_overlap >= 2:
                        emotion = min(5, max(4, int(random.gauss(4.5, 0.5))))
                    elif is_preferred or tag_overlap >= 1:
                        emotion = min(5, max(3, int(random.gauss(3.8, 0.7))))
                    else:
                        emotion = min(5, max(1, int(random.gauss(2.8, 1.0))))
                    
                    completed_at = (assigned_date + timedelta(hours=random.randint(1, 24))).isoformat()
                    status = "COMPLETED"
                else:
                    emotion = None
                    completed_at = None
                    status = "ASSIGNED"
                
                interactions.append({
                    "user_code": user["user_code"],
                    "challenge_master_id": c["challenge_code"],
                    "challenge_status": status,
                    "emotion": emotion,
                    "origin": "RECOMMENDED" if random.random() > 0.3 else "SELF",
                    "assigned_date": assigned_date.strftime("%Y-%m-%d"),
                    "completed_at": completed_at
                })
        
        return interactions
    
    def generate_full_dataset(
        self, 
        interactions_per_user: int = 20
    ) -> DummyDataset:
        """전체 데이터셋 생성"""
        users = self.generate_users()
        challenges = self.generate_challenges()
        interactions = self.generate_interactions(
            users, challenges, interactions_per_user
        )
        
        completed_count = len([i for i in interactions if i["challenge_status"] == "COMPLETED"])
        
        return DummyDataset(
            users=users,
            challenges=challenges,
            interactions=interactions,
            metadata={
                "num_users": len(users),
                "num_challenges": len(challenges),
                "num_interactions": len(interactions),
                "num_completed": completed_count,
                "completion_rate": round(completed_count / len(interactions), 4),
                "generated_at": datetime.now().isoformat(),
                "seed": 42
            }
        )
    
    def generate_train_test_split(
        self,
        test_ratio: float = 0.2
    ) -> Tuple[DummyDataset, DummyDataset]:
        """학습/테스트 데이터 분리"""
        dataset = self.generate_full_dataset()
        
        # 유저 기준 분리
        users = dataset.users
        random.shuffle(users)
        
        split_idx = int(len(users) * (1 - test_ratio))
        train_users = users[:split_idx]
        test_users = users[split_idx:]
        
        train_user_codes = {u["user_code"] for u in train_users}
        test_user_codes = {u["user_code"] for u in test_users}
        
        train_interactions = [
            i for i in dataset.interactions 
            if i["user_code"] in train_user_codes
        ]
        test_interactions = [
            i for i in dataset.interactions 
            if i["user_code"] in test_user_codes
        ]
        
        train_dataset = DummyDataset(
            users=train_users,
            challenges=dataset.challenges,
            interactions=train_interactions,
            metadata={
                **dataset.metadata,
                "split": "train",
                "num_users": len(train_users),
                "num_interactions": len(train_interactions)
            }
        )
        
        test_dataset = DummyDataset(
            users=test_users,
            challenges=dataset.challenges,
            interactions=test_interactions,
            metadata={
                **dataset.metadata,
                "split": "test",
                "num_users": len(test_users),
                "num_interactions": len(test_interactions)
            }
        )
        
        return train_dataset, test_dataset
    
    def prepare_lightfm_data(
        self,
        dataset: DummyDataset
    ) -> Dict:
        """
        LightFM 학습용 데이터 준비
        
        Returns:
            {
                "interactions": [(user_code, challenge_id, rating), ...],
                "user_features": {user_code: [features], ...},
                "item_features": {challenge_id: [features], ...}
            }
        """
        # Interaction 데이터 (완료된 것만)
        interactions = [
            (i["user_code"], i["challenge_master_id"], i["emotion"])
            for i in dataset.interactions
            if i["challenge_status"] == "COMPLETED" and i["emotion"] is not None
        ]
        
        # User Features
        user_features = {}
        for user in dataset.users:
            features = []
            features.append(f"recovery_level:{user['recovery_level']}")
            features.extend([f"survey:{tag}" for tag in user["survey_tags"]])
            features.extend([f"interest:{tag}" for tag in user["interest_tags"]])
            user_features[user["user_code"]] = features
        
        # Item Features
        item_features = {}
        for challenge in dataset.challenges:
            features = []
            features.append(f"category:{challenge['category']}")
            features.append(f"difficulty:{challenge['difficulty_level']}")
            features.extend([f"tag:{tag}" for tag in challenge["tags"]])
            item_features[challenge["challenge_code"]] = features
        
        return {
            "interactions": interactions,
            "user_features": user_features,
            "item_features": item_features,
            "all_user_features": self._extract_all_features(user_features),
            "all_item_features": self._extract_all_features(item_features)
        }
    
    def _extract_all_features(self, feature_dict: Dict) -> Set[str]:
        """모든 고유 피처 추출"""
        all_features = set()
        for features in feature_dict.values():
            all_features.update(features)
        return all_features


def create_sample_dataset() -> DummyDataset:
    """샘플 데이터셋 생성 (테스트용)"""
    generator = DummyDataGenerator(num_users=100, num_challenges=50)
    return generator.generate_full_dataset(interactions_per_user=15)


if __name__ == "__main__":
    generator = DummyDataGenerator(num_users=500, num_challenges=100)
    dataset = generator.generate_full_dataset()
    
    print("=" * 60)
    print("Dummy Dataset Generated")
    print("=" * 60)
    print(f"Users: {dataset.metadata['num_users']}")
    print(f"Challenges: {dataset.metadata['num_challenges']}")
    print(f"Interactions: {dataset.metadata['num_interactions']}")
    print(f"Completed: {dataset.metadata['num_completed']}")
    print(f"Completion Rate: {dataset.metadata['completion_rate']:.2%}")
    print("=" * 60)
    
    # LightFM 데이터 준비 테스트
    lightfm_data = generator.prepare_lightfm_data(dataset)
    print(f"LightFM Interactions: {len(lightfm_data['interactions'])}")
    print(f"User Features: {len(lightfm_data['all_user_features'])}")
    print(f"Item Features: {len(lightfm_data['all_item_features'])}")

// MongoDB 초기화 스크립트: 챌린지 데이터 자동 삽입
// Docker 컨테이너 시작 시 자동 실행됨

print('===== 챌린지 데이터 초기화 시작 =====');

db = db.getSiblingDB('onestep_db');

if (db.challenges.countDocuments() > 0) {
    print('기존 challenges 컬렉션 삭제 중...');
    db.challenges.drop();
}

const challengesData = [
  {
    "challenge_code": 1,
    "title": "오늘 기분을 한 문장으로 적기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["일기", "감정표현"],
    "reward": 10
  },
  {
    "challenge_code": 2,
    "title": "감사한 일 한 가지 떠올리기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["감사", "마음챙김"],
    "reward": 10
  },
  {
    "challenge_code": 3,
    "title": "창문 열어 신선한 공기 마시기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["호흡", "휴식"],
    "reward": 10
  },
  {
    "challenge_code": 4,
    "title": "좋아하는 음악 1곡 듣기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["음악", "휴식"],
    "reward": 10
  },
  {
    "challenge_code": 5,
    "title": "2-3분 호흡 운동하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["호흡", "명상"],
    "reward": 10
  },
  {
    "challenge_code": 6,
    "title": "눈 감고 잠시 아무 생각 안 하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["명상", "휴식"],
    "reward": 10
  },
  {
    "challenge_code": 7,
    "title": "오늘 하루 잘한 일 하나 생각하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["긍정", "자기성찰"],
    "reward": 10
  },
  {
    "challenge_code": 8,
    "title": "좋아하는 사진 한 장 보기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["추억", "사진"],
    "reward": 10
  },
  {
    "challenge_code": 9,
    "title": "좋았던 기억 떠올리며 미소 짓기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["긍정", "추억"],
    "reward": 10
  },
  {
    "challenge_code": 10,
    "title": "부정적인 문장 하나를 쓰고 그 아래 \"그럴 수도 있지\"라고 적기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["긍정", "마음챙김"],
    "reward": 10
  },
  {
    "challenge_code": 11,
    "title": "오늘의 기분을 색깔로 표현하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["감정표현", "색깔"],
    "reward": 10
  },
  {
    "challenge_code": 12,
    "title": "하루를 시작하며 긍정 확언하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["긍정", "자기암시"],
    "reward": 10
  },
  {
    "challenge_code": 13,
    "title": "좋아하는 향기 맡기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["휴식", "감각"],
    "reward": 10
  },
  {
    "challenge_code": 14,
    "title": "유튜브에서 재미있는 짧은 영상 보기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["영상", "휴식"],
    "reward": 10
  },
  {
    "challenge_code": 15,
    "title": "좋아하는 노래 가사 한 줄 적기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["음악", "글쓰기"],
    "reward": 10
  },
  {
    "challenge_code": 16,
    "title": "오늘 나에게 필요한 것 한 가지 메모장에 적기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["자기성찰", "마음챙김"],
    "reward": 10
  },
  {
    "challenge_code": 17,
    "title": "걱정거리를 종이에 적고 접어두기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["글쓰기", "마음정리"],
    "reward": 10
  },
  {
    "challenge_code": 18,
    "title": "난 할 수 있어 라고 말해보기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["마음챙김", "명상"],
    "reward": 10
  },
  {
    "challenge_code": 19,
    "title": "스스로에게 따뜻한 말 한마디 하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["자기사랑", "긍정"],
    "reward": 10
  },
  {
    "challenge_code": 20,
    "title": "하루 목표 하나만 정하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["목표", "계획"],
    "reward": 10
  },
  {
    "challenge_code": 21,
    "title": "물 한 잔 마시기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["수분섭취", "건강"],
    "reward": 10
  },
  {
    "challenge_code": 22,
    "title": "제자리에서 기지개 펴기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["스트레칭", "운동"],
    "reward": 10
  },
  {
    "challenge_code": 23,
    "title": "침대에서 간단한 스트레칭하기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["스트레칭", "휴식"],
    "reward": 10
  },
  {
    "challenge_code": 24,
    "title": "세수하고 얼굴 보습하기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["자기관리", "건강"],
    "reward": 10
  },
  {
    "challenge_code": 25,
    "title": "손목과 발목 돌리기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["스트레칭", "운동"],
    "reward": 10
  },
  {
    "challenge_code": 26,
    "title": "방 안에서 천천히 걷기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["걷기", "운동"],
    "reward": 10
  },
  {
    "challenge_code": 27,
    "title": "이불 정리하기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["정리", "자기관리"],
    "reward": 10
  },
  {
    "challenge_code": 28,
    "title": "창가에서 햇빛 3분 쬐기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["햇빛", "건강"],
    "reward": 10
  },
  {
    "challenge_code": 29,
    "title": "머리 빗기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["자기관리", "정돈"],
    "reward": 10
  },
  {
    "challenge_code": 30,
    "title": "양치질하기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["자기관리", "건강"],
    "reward": 10
  },
  {
    "challenge_code": 31,
    "title": "침대 밖으로 나오기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["시작", "의지"],
    "reward": 10
  },
  {
    "challenge_code": 32,
    "title": "간단한 아침 식사 먹기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["식사", "건강"],
    "reward": 10
  },
  {
    "challenge_code": 33,
    "title": "뭐라도 한 입 먹기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["식사", "건강"],
    "reward": 10
  },
  {
    "challenge_code": 34,
    "title": "손 씻기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["위생", "건강"],
    "reward": 10
  },
  {
    "challenge_code": 35,
    "title": "목 스트레칭 10번 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["스트레칭", "운동"],
    "reward": 10
  },
  {
    "challenge_code": 36,
    "title": "어깨 으쓱하기 10번",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["스트레칭", "운동"],
    "reward": 10
  },
  {
    "challenge_code": 37,
    "title": "앉은 자리에서 발가락 움직이기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["운동", "미세운동"],
    "reward": 10
  },
  {
    "challenge_code": 38,
    "title": "복용 중인 약 챙겨 먹기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["건강", "관리"],
    "reward": 10
  },
  {
    "challenge_code": 39,
    "title": "책상 위 컵 하나 싱크대에 가져다 두기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["정리", "청소"],
    "reward": 10
  },
  {
    "challenge_code": 40,
    "title": "신선한 공기 마시러 창문 열기",
    "category": "LIFESTYLE",
    "difficulty_level": 1,
    "tags": ["환기", "건강"],
    "reward": 10
  },
  {
    "challenge_code": 41,
    "title": "가족에게 \"잘 자\" 인사하기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["가족", "소통"],
    "reward": 10
  },
  {
    "challenge_code": 42,
    "title": "친구에게 이모티콘 하나 보내기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["친구", "소통"],
    "reward": 10
  },
  {
    "challenge_code": 43,
    "title": "좋아하는 게시물에 좋아요 누르기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["SNS", "소통"],
    "reward": 10
  },
  {
    "challenge_code": 44,
    "title": "반려동물이나 식물에게 말 걸기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["반려동물", "식물"],
    "reward": 10
  },
  {
    "challenge_code": 45,
    "title": "가족에게 \"고마워\" 한 마디 하기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["가족", "감사"],
    "reward": 10
  },
  {
    "challenge_code": 46,
    "title": "가족에게 \"좋은 아침\" 인사하기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["가족", "소통"],
    "reward": 10
  },
  {
    "challenge_code": 47,
    "title": "누군가와 눈 맞추고 미소 짓기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["소통", "긍정"],
    "reward": 10
  },
  {
    "challenge_code": 48,
    "title": "온라인에서 응원 댓글 남기기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["온라인", "소통"],
    "reward": 10
  },
  {
    "challenge_code": 49,
    "title": "원스텝에 로그인하기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["루틴", "습관"],
    "reward": 10
  },
  {
    "challenge_code": 50,
    "title": "오늘 기분을 한 줄로 SNS에 남기기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["SNS", "감정표현"],
    "reward": 10
  },
  {
    "challenge_code": 51,
    "title": "친구의 근황 게시물 확인하기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["친구", "관심"],
    "reward": 10
  },
  {
    "challenge_code": 52,
    "title": "커뮤니티 글에 '추천' 또는 '좋아요' 누르기",
    "category": "SOCIAL",
    "difficulty_level": 1,
    "tags": ["온라인", "소통"],
    "reward": 10
  },
  {
    "challenge_code": 53,
    "title": "낙서 한 장 그리기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["그림", "낙서"],
    "reward": 10
  },
  {
    "challenge_code": 54,
    "title": "간단한 메모 남기기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["글쓰기", "기록"],
    "reward": 10
  },
  {
    "challenge_code": 55,
    "title": "좋아하는 색연필로 색칠하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["색칠", "그림"],
    "reward": 10
  },
  {
    "challenge_code": 56,
    "title": "하루를 이모티콘으로 표현하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["감정표현", "창의성"],
    "reward": 10
  },
  {
    "challenge_code": 57,
    "title": "좋아하는 음식 사진 찍기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["사진", "음식"],
    "reward": 10
  },
  {
    "challenge_code": 58,
    "title": "마음 가는 대로 끄적이기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["글쓰기", "자유"],
    "reward": 10
  },
  {
    "challenge_code": 59,
    "title": "눈에 보이는 사물 간단한 그림 스케치하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["그림", "스케치"],
    "reward": 10
  },
  {
    "challenge_code": 60,
    "title": "플레이리스트에 노래 한 곡 추가하기",
    "category": "INNER",
    "difficulty_level": 1,
    "tags": ["음악", "플레이리스트"],
    "reward": 10
  },
  {
    "challenge_code": 61,
    "title": "5분 호흡 명상하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["명상", "호흡"],
    "reward": 20
  },
  {
    "challenge_code": 62,
    "title": "오늘 기분 일기 3줄 쓰기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["일기", "감정표현"],
    "reward": 20
  },
  {
    "challenge_code": 63,
    "title": "감사 일기 3가지 적기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["감사", "일기"],
    "reward": 20
  },
  {
    "challenge_code": 64,
    "title": "10분간 좋아하는 책 읽기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["독서", "휴식"],
    "reward": 20
  },
  {
    "challenge_code": 65,
    "title": "유튜브 명상 영상 따라하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["명상", "영상"],
    "reward": 20
  },
  {
    "challenge_code": 66,
    "title": "하루 10분 디지털 디톡스",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["디톡스", "휴식"],
    "reward": 20
  },
  {
    "challenge_code": 67,
    "title": "좋아하는 영화 한 편 보기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["영화", "휴식"],
    "reward": 20
  },
  {
    "challenge_code": 68,
    "title": "오늘의 우선순위 3가지 적기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["계획", "목표"],
    "reward": 20
  },
  {
    "challenge_code": 69,
    "title": "30분 책 읽기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["독서", "학습"],
    "reward": 20
  },
  {
    "challenge_code": 70,
    "title": "하루 루틴 계획 세우기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["계획", "루틴"],
    "reward": 20
  },
  {
    "challenge_code": 71,
    "title": "걱정거리 3가지 적고 접어두기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["마음정리", "글쓰기"],
    "reward": 20
  },
  {
    "challenge_code": 72,
    "title": "자기 전 잘한 일 떠올리기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["긍정", "자기성찰"],
    "reward": 20
  },
  {
    "challenge_code": 73,
    "title": "마음챙김 운동 10분 하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["마음챙김", "명상"],
    "reward": 20
  },
  {
    "challenge_code": 74,
    "title": "팟캐스트 한 편 듣기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["팟캐스트", "학습"],
    "reward": 20
  },
  {
    "challenge_code": 75,
    "title": "오늘 배운 것 한 가지 적기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["학습", "기록"],
    "reward": 20
  },
  {
    "challenge_code": 76,
    "title": "좋아하는 시 한 편 읽기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["시", "독서"],
    "reward": 20
  },
  {
    "challenge_code": 77,
    "title": "일일 루틴 목록 수행하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["루틴", "습관"],
    "reward": 20
  },
  {
    "challenge_code": 78,
    "title": "내일 할 일 3가지 적기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["계획", "목표"],
    "reward": 20
  },
  {
    "challenge_code": 79,
    "title": "목표 관련 자료 하나 찾아보기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["목표", "실행"],
    "reward": 20
  },
  {
    "challenge_code": 80,
    "title": "짧은 글 읽기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["학습", "언어"],
    "reward": 20
  },
  {
    "challenge_code": 81,
    "title": "5분 마음챙김 연습하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["마음챙김", "명상"],
    "reward": 20
  },
  {
    "challenge_code": 82,
    "title": "책 20페이지 읽어보기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["독서", "학습"],
    "reward": 20
  },
  {
    "challenge_code": 83,
    "title": "화가 났을 때 글로 풀어쓰기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["감정표현", "글쓰기"],
    "reward": 20
  },
  {
    "challenge_code": 84,
    "title": "스트레스 받는 일 종이에 적기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["스트레스", "글쓰기"],
    "reward": 20
  },
  {
    "challenge_code": 85,
    "title": "10분 요가 스트레칭",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["요가", "스트레칭"],
    "reward": 20
  },
  {
    "challenge_code": 86,
    "title": "5분 홈트레이닝",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["운동", "홈트"],
    "reward": 20
  },
  {
    "challenge_code": 87,
    "title": "스쿼트 10개 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["운동", "스쿼트"],
    "reward": 20
  },
  {
    "challenge_code": 88,
    "title": "아침 스트레칭 루틴",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["스트레칭", "아침루틴"],
    "reward": 20
  },
  {
    "challenge_code": 89,
    "title": "집 청소 15분 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["청소", "정리"],
    "reward": 20
  },
  {
    "challenge_code": 90,
    "title": "플랭크 30초 3세트",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["플랭크", "코어운동"],
    "reward": 20
  },
  {
    "challenge_code": 91,
    "title": "점프잭 20개 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["운동", "유산소"],
    "reward": 20
  },
  {
    "challenge_code": 92,
    "title": "물 1.5리터 마시기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["수분섭취", "건강"],
    "reward": 20
  },
  {
    "challenge_code": 93,
    "title": "바른 자세로 30분 앉기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["자세", "건강"],
    "reward": 20
  },
  {
    "challenge_code": 94,
    "title": "10분 전신 스트레칭",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["스트레칭", "운동"],
    "reward": 20
  },
  {
    "challenge_code": 95,
    "title": "런지 10개 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["운동", "하체"],
    "reward": 20
  },
  {
    "challenge_code": 96,
    "title": "벽 푸시업 10개 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["운동", "상체"],
    "reward": 20
  },
  {
    "challenge_code": 97,
    "title": "자전거 크런치 15개",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["운동", "복근"],
    "reward": 20
  },
  {
    "challenge_code": 98,
    "title": "수분 섭취 목표 달성하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["수분섭취", "건강"],
    "reward": 20
  },
  {
    "challenge_code": 99,
    "title": "방 정리 10분 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["정리", "청소"],
    "reward": 20
  },
  {
    "challenge_code": 100,
    "title": "침대 시트 갈기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["청소", "정리"],
    "reward": 20
  },
  {
    "challenge_code": 101,
    "title": "설거지 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["청소", "정리"],
    "reward": 20
  },
  {
    "challenge_code": 102,
    "title": "쓰레기 버리기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["청소", "정리"],
    "reward": 20
  },
  {
    "challenge_code": 103,
    "title": "건강한 식사 준비하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["요리", "건강"],
    "reward": 20
  },
  {
    "challenge_code": 104,
    "title": "반려동물 공간 청소하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["청소", "반려동물"],
    "reward": 20
  },
  {
    "challenge_code": 105,
    "title": "잠자리 들기 전 스트레칭",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["스트레칭", "수면"],
    "reward": 20
  },
  {
    "challenge_code": 106,
    "title": "아침에 물 한 잔 마시기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["수분섭취", "아침루틴"],
    "reward": 20
  },
  {
    "challenge_code": 107,
    "title": "정해진 시간에 잠자리 들기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["수면", "루틴"],
    "reward": 20
  },
  {
    "challenge_code": 108,
    "title": "취침 전 핸드폰 보지 않기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["수면", "디지털디톡스"],
    "reward": 20
  },
  {
    "challenge_code": 109,
    "title": "침실 환기하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["환기", "건강"],
    "reward": 20
  },
  {
    "challenge_code": 110,
    "title": "친구에게 안부 메시지 보내기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["친구", "메시지"],
    "reward": 20
  },
  {
    "challenge_code": 111,
    "title": "가족과 10분 대화하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["가족", "대화"],
    "reward": 20
  },
  {
    "challenge_code": 112,
    "title": "온라인 커뮤니티에 댓글 달기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["온라인", "소통"],
    "reward": 20
  },
  {
    "challenge_code": 113,
    "title": "오래된 친구에게 근황 물어보기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["친구", "연락"],
    "reward": 20
  },
  {
    "challenge_code": 114,
    "title": "가족에게 전화 5분 하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["가족", "전화"],
    "reward": 20
  },
  {
    "challenge_code": 115,
    "title": "친구에게 응원 메시지 보내기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["친구", "응원"],
    "reward": 20
  },
  {
    "challenge_code": 116,
    "title": "온라인 모임 참석하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["온라인", "모임"],
    "reward": 20
  },
  {
    "challenge_code": 117,
    "title": "오늘 하루 만난 사람에게 먼저 가벼운 인사 건네기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["소통", "관계"],
    "reward": 20
  },
  {
    "challenge_code": 118,
    "title": "친구에게 재미있는 콘텐츠 공유하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["친구", "공유"],
    "reward": 20
  },
  {
    "challenge_code": 119,
    "title": "가족 구성원과 체크인하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["가족", "소통"],
    "reward": 20
  },
  {
    "challenge_code": 120,
    "title": "SNS 대신 직접 찍은 사진첩 구경하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["디지털디톡스", "소통"],
    "reward": 20
  },
  {
    "challenge_code": 121,
    "title": "온라인에서 친절한 행동하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["친절", "소통"],
    "reward": 20
  },
  {
    "challenge_code": 122,
    "title": "사람 많은 곳 대신 한적한 공원 산책하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["건강", "안전"],
    "reward": 20
  },
  {
    "challenge_code": 123,
    "title": "친구나 가족 사진 찍기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["사진", "가족", "친구"],
    "reward": 20
  },
  {
    "challenge_code": 124,
    "title": "누군가를 도와주기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["친절", "나눔"],
    "reward": 20
  },
  {
    "challenge_code": 125,
    "title": "감사 인사 전하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["감사", "소통"],
    "reward": 20
  },
  {
    "challenge_code": 126,
    "title": "가족과 함께 영화 보기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["가족", "영화"],
    "reward": 20
  },
  {
    "challenge_code": 127,
    "title": "친구에게 선물 준비하기",
    "category": "SOCIAL",
    "difficulty_level": 2,
    "tags": ["친구", "선물"],
    "reward": 20
  },
  {
    "challenge_code": 128,
    "title": "오늘 일을 만화로 그리기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["그림", "일기"],
    "reward": 20
  },
  {
    "challenge_code": 129,
    "title": "좋아하는 노래 따라 부르기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["노래", "음악"],
    "reward": 20
  },
  {
    "challenge_code": 130,
    "title": "짧은 시 한 편 쓰기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["글쓰기", "시"],
    "reward": 20
  },
  {
    "challenge_code": 131,
    "title": "색칠공부 15분 하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["색칠", "그림"],
    "reward": 20
  },
  {
    "challenge_code": 132,
    "title": "간단한 요리 레시피 따라하기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["요리", "음식"],
    "reward": 20
  },
  {
    "challenge_code": 133,
    "title": "새로운 플레이리스트 만들기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["음악", "플레이리스트"],
    "reward": 20
  },
  {
    "challenge_code": 134,
    "title": "오늘 먹은 음식 사진 찍고 기록하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["음식", "사진", "기록"],
    "reward": 20
  },
  {
    "challenge_code": 135,
    "title": "간단한 DIY 프로젝트 시작하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["DIY", "만들기"],
    "reward": 20
  },
  {
    "challenge_code": 136,
    "title": "방 안 작은 것 꾸미기",
    "category": "LIFESTYLE",
    "difficulty_level": 2,
    "tags": ["꾸미기", "공간"],
    "reward": 20
  },
  {
    "challenge_code": 137,
    "title": "일기를 그림으로 표현하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["그림", "일기"],
    "reward": 20
  },
  {
    "challenge_code": 138,
    "title": "좋아하는 레시피 찾아보기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["요리", "탐색"],
    "reward": 20
  },
  {
    "challenge_code": 139,
    "title": "취미 관련 영상 시청하기",
    "category": "INNER",
    "difficulty_level": 2,
    "tags": ["취미", "학습"],
    "reward": 20
  },
  {
    "challenge_code": 140,
    "title": "실내 자전거 30분 타기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["자전거", "유산소", "운동"],
    "reward": 30
  },
  {
    "challenge_code": 141,
    "title": "홈 PT 프로그램 40분 따라하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["홈트", "운동"],
    "reward": 30
  },
  {
    "challenge_code": 142,
    "title": "온라인 필라테스 수업 듣기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["필라테스", "운동"],
    "reward": 30
  },
  {
    "challenge_code": 143,
    "title": "댄스 유튜브 따라하기 30분",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["댄스", "운동"],
    "reward": 30
  },
  {
    "challenge_code": 144,
    "title": "플랭크 1분 3세트 도전",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["플랭크", "코어운동"],
    "reward": 30
  },
  {
    "challenge_code": 145,
    "title": "30분 운동 루틴 완수하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["운동", "루틴"],
    "reward": 30
  },
  {
    "challenge_code": 146,
    "title": "새로운 운동 종류 시도하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["운동", "도전"],
    "reward": 30
  },
  {
    "challenge_code": 147,
    "title": "운동 목표 달성하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["운동", "목표"],
    "reward": 30
  },
  {
    "challenge_code": 148,
    "title": "10분 고강도 인터벌 운동",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["운동", "고강도"],
    "reward": 30
  },
  {
    "challenge_code": 149,
    "title": "점핑잭 50개 연속 도전",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["유산소", "운동"],
    "reward": 30
  },
  {
    "challenge_code": 150,
    "title": "버피 테스트 20개 도전",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["버피", "운동", "고강도"],
    "reward": 30
  },
  {
    "challenge_code": 151,
    "title": "실내 계단 오르기 10분",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["계단", "운동", "유산소"],
    "reward": 30
  },
  {
    "challenge_code": 152,
    "title": "스쿼트 점프 30개 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["하체", "운동", "고강도"],
    "reward": 30
  },
  {
    "challenge_code": 153,
    "title": "마운틴 클라이머 50개 하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["코어", "운동"],
    "reward": 30
  },
  {
    "challenge_code": 154,
    "title": "전신 서킷 트레이닝 30분",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["서킷", "운동", "전신"],
    "reward": 30
  },
  {
    "challenge_code": 155,
    "title": "요가 플로우 40분 완성하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["요가", "운동"],
    "reward": 30
  },
  {
    "challenge_code": 156,
    "title": "복근 운동 루틴 20분",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["복근", "운동", "코어"],
    "reward": 30
  },
  {
    "challenge_code": 157,
    "title": "하체 근력 운동 30분",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["하체", "운동", "근력"],
    "reward": 30
  },
  {
    "challenge_code": 158,
    "title": "상체 근력 운동 30분",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["상체", "운동", "근력"],
    "reward": 30
  },
  {
    "challenge_code": 159,
    "title": "집 전체 대청소하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["청소", "정리", "활동"],
    "reward": 30
  },
  {
    "challenge_code": 160,
    "title": "옷장 정리 및 정돈하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["정리", "정돈"],
    "reward": 30
  },
  {
    "challenge_code": 161,
    "title": "방 정리 및 안쓰는 물건 정리하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["정리", "공간재배치"],
    "reward": 30
  },
  {
    "challenge_code": 162,
    "title": "한 주 식단 계획하고 준비하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["요리", "식단", "계획"],
    "reward": 30
  },
  {
    "challenge_code": 163,
    "title": "복잡한 요리 레시피 도전하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["요리", "도전"],
    "reward": 30
  },
  {
    "challenge_code": 164,
    "title": "하루 3끼 모두 직접 요리하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["요리", "자취", "건강"],
    "reward": 30
  },
  {
    "challenge_code": 165,
    "title": "빨래 및 다림질 완료하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["가사", "정리"],
    "reward": 30
  },
  {
    "challenge_code": 166,
    "title": "냉장고 정리 및 청소하기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["청소", "정리", "주방"],
    "reward": 30
  },
  {
    "challenge_code": 167,
    "title": "가구 배치 구조 바꾸기",
    "category": "LIFESTYLE",
    "difficulty_level": 3,
    "tags": ["정리", "공간"],
    "reward": 30
  },
  {
    "challenge_code": 168,
    "title": "30분 명상 도전",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["명상", "집중"],
    "reward": 30
  },
  {
    "challenge_code": 169,
    "title": "자기계발 책 1챕터 읽고 요약하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["독서", "자기계발"],
    "reward": 30
  },
  {
    "challenge_code": 170,
    "title": "TED 강연 1개 시청하고 핵심 내용 3줄 요약하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["TED", "학습"],
    "reward": 30
  },
  {
    "challenge_code": 171,
    "title": "장기 목표 구체적으로 계획 세우기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["목표", "계획"],
    "reward": 30
  },
  {
    "challenge_code": 172,
    "title": "일주일 회고록 작성하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["회고", "글쓰기"],
    "reward": 30
  },
  {
    "challenge_code": 173,
    "title": "감사 일기 4가지 이상 적기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["감사", "일기"],
    "reward": 30
  },
  {
    "challenge_code": 174,
    "title": "긴 에세이 한 편 읽고 감상문 쓰기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["독서", "글쓰기"],
    "reward": 30
  },
  {
    "challenge_code": 175,
    "title": "온라인 강의 1시간 수강하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["학습", "온라인"],
    "reward": 30
  },
  {
    "challenge_code": 176,
    "title": "새로운 언어 공부 1시간",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["학습", "언어"],
    "reward": 30
  },
  {
    "challenge_code": 177,
    "title": "장편 다큐멘터리 시청하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["학습", "영상"],
    "reward": 30
  },
  {
    "challenge_code": 178,
    "title": "그림 한 작품 완성하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["그림", "예술"],
    "reward": 30
  },
  {
    "challenge_code": 179,
    "title": "블로그 글 한 편 쓰기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["글쓰기", "블로그"],
    "reward": 30
  },
  {
    "challenge_code": 180,
    "title": "악기 연습 30분",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["악기", "음악"],
    "reward": 30
  },
  {
    "challenge_code": 181,
    "title": "새로운 취미 입문 영상 시청하고 첫 단계 따라하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["취미", "도전"],
    "reward": 30
  },
  {
    "challenge_code": 182,
    "title": "좋아하는 시인의 시집 10페이지 읽기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["시", "독서", "글쓰기"],
    "reward": 30
  },
  {
    "challenge_code": 183,
    "title": "창작 시놉시스 한 페이지 작성하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["글쓰기", "창작"],
    "reward": 30
  },
  {
    "challenge_code": 184,
    "title": "사진 편집 프로젝트 완성하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["사진", "편집"],
    "reward": 30
  },
  {
    "challenge_code": 185,
    "title": "영상 편집 연습 1시간",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["영상", "편집", "학습"],
    "reward": 30
  },
  {
    "challenge_code": 186,
    "title": "프로그래밍 튜토리얼 완수하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["코딩", "학습"],
    "reward": 30
  },
  {
    "challenge_code": 187,
    "title": "디자인 프로젝트 하나 완성하기",
    "category": "INNER",
    "difficulty_level": 3,
    "tags": ["디자인", "창작"],
    "reward": 30
  },
  {
    "challenge_code": 188,
    "title": "가족과 1시간 깊은 대화하기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["가족", "대화", "소통"],
    "reward": 30
  },
  {
    "challenge_code": 189,
    "title": "친구와 화상 통화 30분 이상 하기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["친구", "화상통화"],
    "reward": 30
  },
  {
    "challenge_code": 190,
    "title": "온라인 스터디 그룹 참여하기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["스터디", "학습", "모임"],
    "reward": 30
  },
  {
    "challenge_code": 191,
    "title": "재능 기부 사이트에서 내가 도와줄 수 있는 일 등록하기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["봉사", "나눔"],
    "reward": 30
  },
  {
    "challenge_code": 192,
    "title": "청년지원센터 상담 신청해보기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["상담", "소통"],
    "reward": 30
  },
  {
    "challenge_code": 193,
    "title": "친구에게 긴 편지 쓰기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["친구", "편지", "소통"],
    "reward": 30
  },
  {
    "challenge_code": 194,
    "title": "가족 행사 준비 및 참여하기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["가족", "행사"],
    "reward": 30
  },
  {
    "challenge_code": 195,
    "title": "온라인 게임 협동 플레이 1시간",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["게임", "협동", "소통"],
    "reward": 30
  },
  {
    "challenge_code": 196,
    "title": "친구와 함께 온라인 강의 듣기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["학습", "친구", "협동"],
    "reward": 30
  },
  {
    "challenge_code": 197,
    "title": "온라인 독서 모임 참석하기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["독서", "모임", "소통"],
    "reward": 30
  },
  {
    "challenge_code": 198,
    "title": "멘토링 세션 참여하기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["멘토링", "학습", "소통"],
    "reward": 30
  },
  {
    "challenge_code": 199,
    "title": "온라인 취미 클래스 수강하기",
    "category": "SOCIAL",
    "difficulty_level": 3,
    "tags": ["취미", "학습", "모임"],
    "reward": 30
  }
];

print(`${challengesData.length}개 챌린지 데이터 삽입 중...`);

db.challenges.insertMany(challengesData);

print(`삽입 완료: ${db.challenges.countDocuments()}개`);

print('인덱스 생성 중...');
db.challenges.createIndex({ "challenge_code": 1 }, { unique: true });
db.challenges.createIndex({ "category": 1 });
db.challenges.createIndex({ "difficulty_level": 1 });
db.challenges.createIndex({ "category": 1, "difficulty_level": 1 });

print('인덱스 생성 완료');

print('\n===== 카테고리별 개수 =====');
db.challenges.aggregate([
    { $group: { _id: "$category", count: { $sum: 1 } } },
    { $sort: { _id: 1 } }
]).forEach(function(doc) {
    print(`${doc._id}: ${doc.count}개`);
});

print('\n===== 난이도별 개수 =====');
db.challenges.aggregate([
    { $group: { _id: "$difficulty_level", count: { $sum: 1 } } },
    { $sort: { _id: 1 } }
]).forEach(function(doc) {
    print(`난이도 ${doc._id}: ${doc.count}개`);
});

print('\n===== 챌린지 데이터 초기화 완료 =====');

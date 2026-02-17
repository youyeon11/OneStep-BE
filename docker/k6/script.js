import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    insecureSkipTLSVerify: true, // SSL 인증서 무시 (Ignore SSL certificate errors)

    stages: [
        { duration: '3m', target: 2000 },
    ],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    let accessToken = '';
    const userCode = uuidv4().substring(0, 16);

    // 1. 게스트 로그인
    group('01_Guest_Login', function () {
        const payload = JSON.stringify({ userCode: userCode });
        const params = { headers: { 'Content-Type': 'application/json' } };
        const res = http.post(`${BASE_URL}/api/v1/auth/guest`, payload, params);

        check(res, {
            'login success': (r) => r.status === 200,
            'has accessToken': (r) => r.json().result.accessToken !== undefined,
        });

        if (res.status !== 200) {
            console.error(`로그인 실패! Status: ${res.status}, Body: ${res.body}`);
            return;
        }

        const body = res.json();
        if (!body.result || !body.result.accessToken) {
            console.error(`예상치 못한 응답 구조! Body: ${res.result}`);
            return;
        }

        accessToken = res.json().result.accessToken;
    });

    const authHeaders = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`,
        },
    };

    // 2. 회원 설문 결과 등록
    group('02_Register_Survey', function () {
        const answers = [];
        for (let i = 1; i <= 15; i++) {
            answers.push(Math.floor(Math.random() * 4)) // 0-3점 랜덤 부여 );
        }

        const totalScore = answers.reduce((acc, cur) => acc + cur, 0);

        const payload = JSON.stringify({ answers: answers, totalScore: totalScore });
        const res = http.post(`${BASE_URL}/api/v1/users/survey`, payload, authHeaders);

        check(res, {
            'survey success': (r) => r.status === 204 || r.status === 200,
        });
    });

    let initialChallenges = [];

    // 3. 초기 유저 20개 항목 반환
    group('03_Get_Initial_Challenges', function () {
        const res = http.get(`${BASE_URL}/api/v1/challenges/init`, authHeaders);

        check(res, {
            'get init success': (r) => r.status === 200,
            'received 20 items': (r) => r.json().result.length >= 20,
        });

        initialChallenges = res.json().result;
    });

    // 4. 초기 유저 5개 선택
    group('04_Choose_5_Challenges', function () {
        // 무작위로 5개 섞어서 선택
        const shuffled = initialChallenges.sort(() => 0.5 - Math.random());
        const selected = shuffled.slice(0, 5);

        const payload = JSON.stringify(selected);
        const res = http.post(`${BASE_URL}/api/v1/challenges/init/choices`, payload, authHeaders);

        check(res, {
            'choice success': (r) => r.status === 200,
        });
    });

    let recommendedChallenges = [];

    // 5. 추천 챌린지 조회
    group('05_Get_Recommended_Challenges', function () {
        const res = http.get(`${BASE_URL}/api/v1/challenges/recommend`, authHeaders);

        check(res, {
            'recommend success': (r) => r.status === 200,
        });

        recommendedChallenges = res.json().result;
    });

    // 6. 챌린지 완료
    group('06_Complete_Challenge', function () {
        if (recommendedChallenges.length > 0) {
            const targetId = recommendedChallenges[0].challengeId;
            const payload = JSON.stringify({
                challengeId: targetId,
                reaction: Math.floor(Math.random() * 5) + 1, // 1-5 사이 랜덤 리액션
            });

            const res = http.post(`${BASE_URL}/api/v1/challenges/complete`, payload, authHeaders);

            check(res, {
                'complete success': (r) => r.status === 200,
                'status is COMPLETED': (r) => r.json().result.challengeStatus === 'COMPLETED',
            });
        }
    });

    sleep(1); // 각 VU 간의 실행 간격 조절
}

INSERT INTO users (
    email,
    user_code,
    recovery_level,
    nickname,
    total_exp,
    last_active_at,
    user_status,
    inactivated_at,
    terms_agree,
    gps_opt_in,
    notif_opt_in
) VALUES
-- 정상 활동 유저
('alice@example.com', 'U1001', 1, 'Alice', 1200, now() - interval '1 day', 'ACTIVE', NULL, true, true, true),

-- 최근 활동한 유저
('bob@example.com', 'U1002', 2, 'Bob', 300, now() - interval '2 hours', 'ACTIVE', NULL, true, false, true),

-- 장기 미접속 유저
('charlie@example.com', 'U1003', 1, 'Charlie', 0, now() - interval '90 days', 'INACTIVE', now() - interval '30 days', true, false, false),

-- 이메일 없는 유저
(NULL, 'U1004', 3, 'Dana', 540, now() - interval '7 days', 'ACTIVE', NULL, true, true, false),

-- 탈퇴 처리된 유저
('eric@example.com', 'U1005', 2, 'Eric', 0, NULL, 'DELETE', now() - interval '10 days', false, NULL, NULL),

-- 약관 미동의 유저
('frank@example.com', 'U1006', 3, 'Frank', 150, now() - interval '3 days', 'ACTIVE', NULL, false, true, true),

-- 높은 회복력 활성 유저
('grace@example.com', 'U1007', 2, 'Grace', 800, now() - interval '5 hours', 'ACTIVE', NULL, true, true, true),

-- 경험치 많은 유저
('henry@example.com', 'U1008', 1, 'Henry', 5000, now() - interval '30 minutes', 'ACTIVE', NULL, true, true, false),

-- 중간 회복력 유저
('iris@example.com', 'U1009', 3, 'Iris', 450, now() - interval '12 hours', 'ACTIVE', NULL, true, false, false),

-- 최근 비활성화된 유저

-- 테스트 유저
('ssafy@test.com', '550E8400E29B', 3, '김싸피', 0, now() - interval '45 days', 'ACTIVE', now() - interval '5 days', true, true, true)
    ON CONFLICT (user_code) DO NOTHING;

-- 테스트 유저의 펫 추가
INSERT INTO pet_ownerships (user_id, is_main, pet_level, max_exp, current_exp, created_at, updated_at)
SELECT
    u.id,
    true,
    1,
    100,  -- PetLevelCalculator.getMaxExpForLevel(1)과 동일하게 설정
    0,
    now() - interval '45 days',
    now() - interval '45 days'
FROM users u
WHERE u.user_code = '550E8400E29B'
ON CONFLICT DO NOTHING;
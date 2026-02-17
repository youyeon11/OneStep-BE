-- 시퀀스 동기화 (ID 충돌 방지)
-- 모든 시드 데이터 삽입 후 실행되어 JPA/Hibernate 저장 시 시퀀스 충돌 방지

DO $$
DECLARE
    seq_val BIGINT;
BEGIN
    -- users
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM users;
    PERFORM setval(pg_get_serial_sequence('users', 'id'), seq_val, false);

    -- challenge_master
    SELECT COALESCE(MAX(challenge_master_id), 0) + 1 INTO seq_val FROM challenge_master;
    PERFORM setval(pg_get_serial_sequence('challenge_master', 'challenge_master_id'), seq_val, false);

    -- challenge_assignments
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM challenge_assignments;
    PERFORM setval(pg_get_serial_sequence('challenge_assignments', 'id'), seq_val, false);

    -- route_levels
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM route_levels;
    PERFORM setval(pg_get_serial_sequence('route_levels', 'id'), seq_val, false);

    -- route_sessions
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM route_sessions;
    PERFORM setval(pg_get_serial_sequence('route_sessions', 'id'), seq_val, false);

    -- letters
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM letters;
    PERFORM setval(pg_get_serial_sequence('letters', 'id'), seq_val, false);

    -- letter_deliveries
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM letter_deliveries;
    PERFORM setval(pg_get_serial_sequence('letter_deliveries', 'id'), seq_val, false);

    -- survey_log
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM survey_log;
    PERFORM setval(pg_get_serial_sequence('survey_log', 'id'), seq_val, false);

    -- interest_question
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM interest_question;
    PERFORM setval(pg_get_serial_sequence('interest_question', 'id'), seq_val, false);

    -- interest_answer
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM interest_answer;
    PERFORM setval(pg_get_serial_sequence('interest_answer', 'id'), seq_val, false);

    -- gp_ledgers
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM gp_ledgers;
    PERFORM setval(pg_get_serial_sequence('gp_ledgers', 'id'), seq_val, false);

    -- pet_ownerships
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM pet_ownerships;
    PERFORM setval(pg_get_serial_sequence('pet_ownerships', 'id'), seq_val, false);

    -- user_signal_logs
    SELECT COALESCE(MAX(log_id), 0) + 1 INTO seq_val FROM user_signal_logs;
    PERFORM setval(pg_get_serial_sequence('user_signal_logs', 'log_id'), seq_val, false);

    -- user_interest_profiles
    SELECT COALESCE(MAX(id), 0) + 1 INTO seq_val FROM user_interest_profiles;
    PERFORM setval(pg_get_serial_sequence('user_interest_profiles', 'id'), seq_val, false);

    RAISE NOTICE 'All sequences synchronized successfully';
END $$;

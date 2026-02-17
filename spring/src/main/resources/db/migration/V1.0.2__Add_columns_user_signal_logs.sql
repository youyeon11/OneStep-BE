-- event_status, weight 추가
ALTER TABLE user_signal_logs
    ADD COLUMN event_status VARCHAR(50),
ADD COLUMN weight NUMERIC(6,5);

-- user_id를 user_code로 변경
ALTER TABLE user_signal_logs
DROP COLUMN user_id,
ADD COLUMN user_code VARCHAR;
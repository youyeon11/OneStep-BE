-- user_code 추가하기
ALTER TABLE gp_ledgers
    ADD COLUMN IF NOT EXISTS user_code VARCHAR;

-- index 추가하기
CREATE INDEX IF NOT EXISTS idx_gp_ledgers_user_code
    ON gp_ledgers (user_code);
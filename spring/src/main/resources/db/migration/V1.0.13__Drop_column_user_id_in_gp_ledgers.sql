-- drop index
DROP INDEX IF EXISTS idx_chat_sessions_user_id;

-- drop column user_id
ALTER TABLE gp_ledgers
DROP COLUMN user_id;
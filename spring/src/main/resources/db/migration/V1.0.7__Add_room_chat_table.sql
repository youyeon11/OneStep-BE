-- drop column
ALTER TABLE room
DROP COLUMN answer;

-- room_message
CREATE TABLE IF NOT EXISTS room_message (
    id BIGSERIAL PRIMARY KEY,
       room_id BIGINT NOT NULL,
       sender_code VARCHAR(50) NOT NULL,
       message_role_type VARCHAR,
       content TEXT NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

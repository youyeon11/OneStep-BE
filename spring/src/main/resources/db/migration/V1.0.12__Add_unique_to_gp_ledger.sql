-- gp_ledgers에 유니크 키 추가
ALTER TABLE gp_ledgers
    ADD CONSTRAINT uk_gp_ledgers_reward_once
        UNIQUE (user_id, reason, ref_type, ref_id);
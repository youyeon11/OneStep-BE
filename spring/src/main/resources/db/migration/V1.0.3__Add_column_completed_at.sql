-- completed_at 추가
ALTER TABLE challenge_assignments
    ADD COLUMN completed_at timestamp NULL;
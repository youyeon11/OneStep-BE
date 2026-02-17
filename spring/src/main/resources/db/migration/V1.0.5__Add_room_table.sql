-- trouble table
CREATE TABLE IF NOT EXISTS trouble (
    id        BIGSERIAL PRIMARY KEY,
    content   TEXT,
    user_code VARCHAR(50) NOT NULL
);

-- room table
CREATE TABLE IF NOT EXISTS room (
    id               BIGSERIAL PRIMARY KEY,
    trouble_id          BIGINT NOT NULL,
    duration_time    INT,
    topic            VARCHAR(100),
    answer           TEXT,
    closed_at        TIMESTAMP
);

-- solution table
CREATE TABLE IF NOT EXISTS solution (
    id bigserial PRIMARY KEY,
    user_code varchar not null,
    trouble_id bigint not null,
    summary varchar not null,
    read_at timestamp
);

CREATE INDEX idx_solution_trouble_id ON solution(trouble_id);
CREATE INDEX idx_trouble_user_code ON trouble(user_code);
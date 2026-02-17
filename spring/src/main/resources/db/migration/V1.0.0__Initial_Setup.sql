-- letters
CREATE TABLE IF NOT EXISTS letters (
    id bigserial NOT NULL,
    user_id bigint NOT NULL,
    filter_status varchar NOT NULL DEFAULT 'PENDING'
    CHECK (filter_status IN ('PENDING', 'PASS', 'FAIL')),
    title varchar NULL,
    content varchar NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_letters PRIMARY KEY (id)
);

-- survey_log
CREATE TABLE IF NOT EXISTS survey_log (
    id bigserial NOT NULL,
    answer int NULL,
    user_id bigint NOT NULL,
    survey_number int NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_survey_log PRIMARY KEY (id)
);

-- interest_question
CREATE TABLE IF NOT EXISTS interest_question (
    id bigserial NOT NULL,
    content varchar(20) NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_interest_question PRIMARY KEY (id)
);

-- interest_answer
CREATE TABLE IF NOT EXISTS interest_answer (
    id bigserial NOT NULL,
    content varchar NULL,
    interest_question_id bigint NULL,
    answer_status varchar NULL,
    user_id bigint NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_interest_answer PRIMARY KEY (id)
);

-- route_sessions
CREATE TABLE IF NOT EXISTS route_sessions (
    id bigserial NOT NULL,
    user_id bigint NOT NULL,
    route_status varchar NOT NULL,
    started_at timestamp NULL,
    ended_at timestamp NULL,
    duration_seconds int NULL,
    destination_grid varchar NULL,
    route_levels_id bigint NOT NULL,
    log_date date NULL,
    content varchar NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_route_sessions PRIMARY KEY (id)
);

-- route_levels
CREATE TABLE IF NOT EXISTS route_levels (
    id bigserial NOT NULL,
    route_level int NULL,
    label varchar NOT NULL,
    recommended_min_distance_m int NULL,
    recommended_max_distance_m int NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_route_levels PRIMARY KEY (id)
);

-- gp_ledgers
CREATE TABLE IF NOT EXISTS gp_ledgers (
    id bigserial NOT NULL,
    reason varchar NOT NULL,
    ref_type varchar NULL,
    ref_id bigint NULL,
    user_id bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_gp_ledgers PRIMARY KEY (id)
);

-- pet_ownerships
CREATE TABLE IF NOT EXISTS pet_ownerships (
    id bigserial NOT NULL,
    user_id bigint NOT NULL,
    is_main boolean NOT NULL,
    pet_level int NOT NULL,
    max_exp int NOT NULL,
    current_exp int NULL,
    pet_code varchar NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_pet_ownerships PRIMARY KEY (id)
);

-- letter_deliveries
CREATE TABLE IF NOT EXISTS letter_deliveries (
    id bigserial NOT NULL,
    delivered_at timestamp NULL,
    storage_status varchar NOT NULL,
    is_read boolean NULL,
    read_at timestamp NULL,
    receiver_id bigint NOT NULL,
    letter_id bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_letter_deliveries PRIMARY KEY (id)
);

-- users
CREATE TABLE IF NOT EXISTS users (
    id bigserial NOT NULL,
    email varchar NULL,
    user_code varchar NOT NULL UNIQUE,
    recovery_level int NOT NULL,
    nickname varchar NOT NULL,
    total_exp int NOT NULL,
    last_active_at timestamp NULL,
    user_status varchar NOT NULL,
    inactivated_at timestamp NULL,
    terms_agree boolean NULL,
    gps_opt_in boolean NULL,
    notif_opt_in boolean NULL,
    is_open boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- challenge_assignments
CREATE TABLE IF NOT EXISTS challenge_assignments (
    id bigserial NOT NULL,
    assigned_date date NOT NULL,
    challenge_status varchar NOT NULL,
    content varchar NULL,
    origin varchar NOT NULL,
    user_id bigint NOT NULL,
    challenge_master_id bigint NULL,
    log_date date NULL,
    emotion int NULL,
    exp int NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_challenge_assignments PRIMARY KEY (id)
);

-- challenge_master
CREATE TABLE IF NOT EXISTS challenge_master (
    challenge_master_id bigserial NOT NULL,
    title varchar(200) NULL,
    category varchar(50) NULL,
    difficulty_level int NULL,
    tags text NULL,
    reward int NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_challenge_master PRIMARY KEY (challenge_master_id)
);

-- chat_sessions
CREATE TABLE IF NOT EXISTS chat_sessions (
    id bigserial NOT NULL,
    user_id bigint NOT NULL,
    chat_status varchar NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_chat_sessions PRIMARY KEY (id)
);

-- user_signal_logs
CREATE TABLE IF NOT EXISTS user_signal_logs (
    log_id bigserial NOT NULL,
    event_type varchar(20) NULL,
    target_id bigint NULL,
    metadata jsonb NULL,
    user_id bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_signal_logs PRIMARY KEY (log_id)
);

-- user_interest_profiles
CREATE TABLE IF NOT EXISTS user_interest_profiles (
    id bigserial NOT NULL,
    keywords jsonb NULL,
    last_analyzed_at timestamp NULL,
    user_id bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_interest_profiles PRIMARY KEY (id)
);

-- chat_messages
CREATE TABLE IF NOT EXISTS chat_messages (
    id bigserial NOT NULL,
    role varchar NOT NULL,
    content text NOT NULL,
    meta jsonb NULL,
    chat_session_id bigint NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_chat_messages PRIMARY KEY (id)
);

-- Add Indexes
CREATE INDEX IF NOT EXISTS idx_users_user_code ON users(user_code);
CREATE INDEX IF NOT EXISTS idx_letters_user_id ON letters(user_id);
CREATE INDEX IF NOT EXISTS idx_survey_log_user_id ON survey_log(user_id);
CREATE INDEX IF NOT EXISTS idx_interest_answer_user_id ON interest_answer(user_id);
CREATE INDEX IF NOT EXISTS idx_route_sessions_user_id ON route_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_route_sessions_levels_id ON route_sessions(route_levels_id);
CREATE INDEX IF NOT EXISTS idx_gp_ledgers_user_id ON gp_ledgers(user_id);
CREATE INDEX IF NOT EXISTS idx_pet_ownerships_user_id ON pet_ownerships(user_id);
CREATE INDEX IF NOT EXISTS idx_letter_deliveries_receiver_id ON letter_deliveries(receiver_id);
CREATE INDEX IF NOT EXISTS idx_letter_deliveries_letter_id ON letter_deliveries(letter_id);
CREATE INDEX IF NOT EXISTS idx_challenge_assignments_user_id ON challenge_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_signal_logs_user_id ON user_signal_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_user_interest_profiles_user_id ON user_interest_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(chat_session_id);
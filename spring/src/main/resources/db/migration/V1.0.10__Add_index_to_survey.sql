-- survey에 유니크 인덱스 추가
CREATE UNIQUE INDEX idx_survey_number
    ON survey(survey_number);
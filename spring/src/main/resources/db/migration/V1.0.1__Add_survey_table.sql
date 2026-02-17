-- survey
CREATE TABLE IF NOT EXISTS survey (
    id bigserial NOT NULL,
    survey_number int NULL,
    content varchar NULL,
    category varchar NULL, -- INNER, LIFESTYLE, SOCIAL
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_survey PRIMARY KEY (id)
);

-- 기본 내용
INSERT INTO survey (
    survey_number,
    content,
    category
) VALUES
      (1, '최근 6개월간 편의점 방문 등 꼭 필요한 경우 외에는 집 밖으로 나가지 않았다.', 'LIFESTYLE'),
      (2, '대부분의 시간을 내 방 안에서만 보낸다.', 'LIFESTYLE'),
      (3, '낮과 밤이 바뀌어 주로 밤에 활동하고 낮에 잠을 잔다.', 'LIFESTYLE'),
      (4, '가족이 집 안에 있을 때는 방 밖으로 나가는 것이 꺼려진다.', 'LIFESTYLE'),
      (5, '특별한 일이 없으면 일주일 내내 한 번도 외출하지 않는다.', 'LIFESTYLE'),

      (6, '속마음을 털어놓거나 고민을 상담할 사람이 한 명도 없다.', 'SOCIAL'),
      (7, '급한 일이 생겼을 때 도움을 요청할 지인이 주변에 없다.', 'SOCIAL'),
      (8, 'SNS나 온라인 게임 외에는 실제로 만나 대화하는 사람이 거의 없다.', 'SOCIAL'),
      (9, '모르는 사람뿐만 아니라 아는 사람을 마주치는 것도 피하고 싶다.', 'SOCIAL'),
      (10, '사회로부터 내가 격리되어 있다는 느낌을 강하게 받는다.', 'SOCIAL'),

      (11, '현재 나의 삶에 대해 무기력함이나 허무함을 자주 느낀다.', 'INNER'),
      (12, '사람들의 시선이 두려워 밖으로 나가는 것이 공포스럽다.', 'INNER'),
      (13, '취업이나 학업 등 미래를 생각하면 막막해서 아무것도 손에 잡히지 않는다.', 'INNER'),
      (14, '내가 사회에서 쓸모없는 존재처럼 느껴질 때가 많다.', 'INNER'),
      (15, '다른 사람들은 잘 사는데 나만 뒤처져 있다는 생각이 든다.', 'INNER')
;
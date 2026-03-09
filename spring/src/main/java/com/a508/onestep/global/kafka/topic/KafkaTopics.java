package com.a508.onestep.global.kafka.topic;

/*
Kafka Topic 이름 관리
 */
public final class KafkaTopics {

    private KafkaTopics() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    /*
    사용자 행동 기반 로그 Topic
     */
    public static final String USER_ROUTINE_GENERATE = "user.routine.generate";

    /*
    방문 여부를 나타내는 userCode 전송
     */
    public static final String VISITED_USER = "visited.user.topic";

    /*
    초기 유저에게 보내는 코드
     */
    public static final String INITIAL_USER_INFO = "initial.user.topic";

    /*
    outbox 이벤트 발행을 위한 토픽
     */
    public static final String OUTBOX_EVENT = "outbox.event.topic";
}

package com.a508.onestep.global.message;

import java.util.Map;

public interface MessageTemplate {

    /**
     * Redis Pub/Sub을 통한 메시지 전송
     * @param destination 목적지
     * @param payload 전송 메시지
     */
    void send(String destination, Object payload);

    /**
     * Redis Pub/Sub을 통한 메시지 전송 + 헤더
     * @param destination 목적지
     * @param payload 전송할 메시지
     * @param headers 추가 헤더
     */
    void send(String destination, Object payload, Map<String, Object> headers);
}

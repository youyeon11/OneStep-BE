package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 메시지 발행 대상 유형
 */
@Getter
@RequiredArgsConstructor
public enum MessageType {

    TOPIC("구독 토픽 브로드캐스트 (/topic/...)"),
    QUEUE("특정 사용자 큐 (/queue/...)"),
    USER("특정 사용자 (/user/...)");

    private final String description;
}

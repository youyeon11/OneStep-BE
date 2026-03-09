package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 아웃박스 패턴 메시지의 전송 상태
 */
@Getter
@RequiredArgsConstructor
public enum OutboxStatus {

    PENDING("전송 대기"),
    SENT("전송 완료"),
    FAILED("전송 실패");

    private final String description;
}

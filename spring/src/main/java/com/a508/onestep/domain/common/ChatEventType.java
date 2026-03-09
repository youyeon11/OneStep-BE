package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 채팅방에서 발생하는 이벤트 유형
 * - 메시지 콘텐츠 유형: {@link #TEXT}, {@link #SYSTEM}, {@link #TIMER}
 * - 입출입 이벤트:     {@link #ENTER}, {@link #EXIT}  (메시지 표시 + 방 상태 변경 공용)
 * - 방 상태 전용:      {@link #CLOSING_SOON}, {@link #CLOSED}
 */
@Getter
@RequiredArgsConstructor
public enum ChatEventType {

    // 메시지 콘텐츠 유형
    TEXT("일반 텍스트"),
    SYSTEM("시스템 메시지"),
    TIMER("타이머 알림"),

    // 입출입 이벤트 (채팅 메시지 표시 + 방 상태 변경 공용)
    ENTER("입장"),
    EXIT("퇴장"),

    // 방 상태 전용 이벤트 (DB에 저장되지 않음)
    CLOSING_SOON("마감 임박"),
    CLOSED("마감");

    private final String description;
}

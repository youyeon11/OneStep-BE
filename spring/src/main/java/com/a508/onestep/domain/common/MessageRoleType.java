package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageRoleType {
    TEXT("일반 텍스트"),
    SYSTEM("시스템 메시지"),
    ENTER("입장 알림"),
    EXIT("퇴장 알림"),
    TIMER("타이머 알림");

    private final String description;
}

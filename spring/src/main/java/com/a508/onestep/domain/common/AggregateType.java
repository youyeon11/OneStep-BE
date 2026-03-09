package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 아웃박스 패턴에서 이벤트를 발행하는 도메인 애그리게이트 유형
 */
@Getter
@RequiredArgsConstructor
public enum AggregateType {

    USER("회원"),
    CHALLENGE("챌린지"),
    ROUTE("산책 경로"),
    LETTER("편지"),
    ROOM("채팅방"),
    PET("펫");

    private final String description;
}

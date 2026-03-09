package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 챌린지 할당의 출처
 */
@Getter
@RequiredArgsConstructor
public enum Origin {

    SELF("직접 등록"),
    RECOMMENDED("AI 추천"),
    ROUTE("산책 경로"),
    LETTER("편지");

    private final String description;
}

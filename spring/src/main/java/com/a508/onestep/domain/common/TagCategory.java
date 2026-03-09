package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 챌린지 마스터 태그 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum TagCategory {

    INNER("내면 활동"),
    LIFESTYLE("생활 습관"),
    SOCIAL("사회적 활동");

    private final String description;
}

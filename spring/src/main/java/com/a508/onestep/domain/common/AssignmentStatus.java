package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 챌린지 과제의 진행 상태
 */
@Getter
@RequiredArgsConstructor
public enum AssignmentStatus {

    ASSIGNED("할당됨"),
    PROGRESS("진행 중"),
    COMPLETED("완료됨"),
    CANCELED("취소됨");

    private final String description;
}

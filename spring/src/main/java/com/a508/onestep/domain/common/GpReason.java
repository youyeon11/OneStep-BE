package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 보상에 대한 원장
 */
@Getter
@RequiredArgsConstructor
public enum GpReason {

    CHALLENGE_COMPLETE_REWARD("챌린지 완료 보상"),
    CHALLENGE_CANCEL("챌린지 취소"),
    LETTER_COMPLETE_REWARD("편지 완료 보상");

    private final String description;
}

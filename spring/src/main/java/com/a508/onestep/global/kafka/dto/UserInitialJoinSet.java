package com.a508.onestep.global.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserInitialJoinSet {
    private final String userCode;
    private final Integer recoveryLevel;
    private final String petNickname;
}

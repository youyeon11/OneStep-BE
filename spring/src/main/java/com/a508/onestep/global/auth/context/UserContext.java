package com.a508.onestep.global.auth.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserContext {
    private final String userCode;
    private final String role;
}
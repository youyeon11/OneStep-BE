package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security 권한 역할
 */
@Getter
@RequiredArgsConstructor
public enum RoleType {

    ROLE_USER("일반 사용자"),
    ROLE_ADMIN("관리자"),
    ROLE_SYSTEM("시스템");

    private final String description;
}

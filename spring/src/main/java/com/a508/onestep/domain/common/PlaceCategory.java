package com.a508.onestep.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 카카오 로컬 API 장소 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum PlaceCategory {

    LARGE_MART("MT1", "대형마트"),
    CONVENIENCE_STORE("CS2", "편의점"),
    CULTURE("CT1", "문화시설"),
    PUBLIC_OFFICE("PO3", "공공기관"),
    TOURIST_SPOT("AT4", "관광명소"),
    RESTAURANT("FD6", "음식점"),
    CAFE("CE7", "카페");

    private final String code;
    private final String description;
}

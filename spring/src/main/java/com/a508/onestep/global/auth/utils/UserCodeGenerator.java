package com.a508.onestep.global.auth.utils;

import java.time.Instant;
import java.util.UUID;

/*
UUID 생성 Util 클래스
 */
public final class UserCodeGenerator {

    private static final int RANDOM_PART_LENGTH = 12;

    private UserCodeGenerator() {}

    /*
    접속 시간과 함께 랜덤 UUID 발급
     */
    public static String generate() {
        // 접속 시간 계산
        String timePart = Long.toString(Instant.now().toEpochMilli(), 36)
                .toUpperCase(); // base36으로 축약

        // 랜덤 UUID
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, RANDOM_PART_LENGTH)
                .toUpperCase();

        return timePart + "-" + randomPart;
    }
}

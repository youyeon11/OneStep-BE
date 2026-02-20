package com.a508.onestep.domain.pet.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// 펫 레벨 계산 메서드
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PetLevelCalculator {

    private static final int MAX_LEVEL = 50;

    /**
     * 누적 경험치로 현재 레벨을 계산합니다.
     */
    public static int getPetLevel(int totalExp) {
        if (totalExp <= 0) return 1;

        int level = 1;
        while (level < MAX_LEVEL) {
            // 다음 레벨 시작 경험치보다 작으면 현재 레벨 유지
            if (totalExp < getLevelStartExp(level + 1)) {
                break;
            }
            level++;
        }
        return level;
    }

    /**
     * 해당 레벨이 시작되는 시점의 누적 경험치 총합을 계산합니다.
     * 1Lv: 0, 2Lv: 100, 3Lv: 100+(100+40)=240 ...
     */
    public static int getLevelStartExp(int level) {
        if (level <= 1) return 0;
        if (level > MAX_LEVEL) level = MAX_LEVEL;

        int accumulatedExp = 0;
        int currentLevelMax = 100; // 1레벨 통의 크기

        for (int i = 1; i < level; i++) {
            accumulatedExp += currentLevelMax;
            // 10레벨 이하까지는 요구량이 40씩 증가, 그 이후는 100씩 증가
            currentLevelMax += (i <= 10) ? 40 : 100;
        }
        return accumulatedExp;
    }

    /**
     * 현재 레벨에서 다음 레벨까지 가기 위한 경험치 통(maxExp)을 계산합니다.
     */
    public static int getMaxExpForLevel(int level) {
        if (level >= MAX_LEVEL) return -1; // 만렙 표시
        return getLevelStartExp(level + 1) - getLevelStartExp(level);
    }

    /**
     * 현재 레벨 내에서 순수하게 쌓인 경험치를 계산합니다.
     */
    public static int getCurrentExpInLevel(int totalExp, int level) {
        return totalExp - getLevelStartExp(level);
    }
}
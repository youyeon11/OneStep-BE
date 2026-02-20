package com.a508.onestep.global.kafka.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserFeatureSet {

    private String userCode;

    private String requestDate;

    private UserContext userContext;

    private List<ChallengeHistory> challengeHistory;

    private RouteInfo routeInfo;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class UserContext {
        private Integer recoveryLevel;

        /*
        디버깅용
         */
        @Override
        public String toString() {
            return "UserContext{" +
                    "recoveryLevel=" + recoveryLevel +
                    '}';
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class ChallengeHistory {
        private Long challengeId;
        private Long challengeMasterId;
        private String challengeStatus;
        private String assignedDate;
        private String completedAt;
        private Integer emotion;
        private String origin;
        private BigDecimal weight;

        /*
        디버깅용
         */
        @Override
        public String toString() {
            return "ChallengeHistory{" +
                    "challengeId=" + challengeId +
                    ", challengeMasterId=" + challengeMasterId +
                    ", challengeStatus='" + challengeStatus + '\'' +
                    ", assignedDate='" + assignedDate + '\'' +
                    ", completedAt='" + completedAt + '\'' +
                    ", emotion=" + emotion +
                    ", origin='" + origin + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class RouteInfo {
        private Long routeSessionId;
        private String content;
        private String completedAt;
        private String challengeStatus;
        private String origin;

        /*
        디버깅용
         */
        @Override
        public String toString() {
            return "RouteInfo{" +
                    "routeSessionId='" + routeSessionId + '\'' +
                    ", content='" + content + '\'' +
                    ", challengeStatus='" + challengeStatus + '\'' +
                    ", origin='" + origin + '\'' +
                    '}';
        }
    }
}

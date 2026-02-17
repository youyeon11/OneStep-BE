package com.a508.onestep.domain.challenge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "특정 날짜의 챌린지 내역 및 산책 사진 조회")
public class DailyDetailResponseDto {

    @Schema(description = "완료된 챌린지 리스트")
    private List<ChallengeAssignmentDto> completedChallenges;

    @Schema(description = "그날의 감정 점수")
    private Long average;

    @Schema(description = "산책 이미지 URL")
    private String walkImageUrl;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "완료된 챌린지 내용")
    public static class ChallengeAssignmentDto{

        @Schema(description = "챌린지 ID")
        private Long challengeId;

        @Schema(description = "챌린지 내용")
        private String content;
    }
}

package com.a508.onestep.domain.challenge.dto.response;

import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
챌린지 조회를 위한 응답 DTO
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "챌린지 응답 DTO")
public class ChallengeResponseDto {

    @Schema(name = "챌린지(할 일)의 PK")
    private Long challengeId;

    @Schema(name = "챌린지 내용")
    private String content;

    @Schema(description = "RECOMMENDED, SELF, ROUTE", example = "RECOMMENDED")
    private String origin;

    @Schema(description = "ASSIGNED, COMPLETED, PROGRESS, CANCELED", example = "ASSIGNED")
    private String challengeStatus;

    @Schema(description = "챌린지 완료 시 받을 보상")
    private Integer exp;

    /*
    응답 DTO로 변환
     */
    public static ChallengeResponseDto from(ChallengeAssignment challengeAssignment) {

        return ChallengeResponseDto.builder()
                .challengeId(challengeAssignment.getId())
                .origin(challengeAssignment.getOrigin().name())
                .content(challengeAssignment.getContent())
                .challengeStatus(challengeAssignment.getChallengeStatus().name())
                .exp(challengeAssignment.getExp())
                .build();
    }
}

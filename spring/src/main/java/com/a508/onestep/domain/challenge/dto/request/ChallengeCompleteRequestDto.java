package com.a508.onestep.domain.challenge.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
챌린지 완료 요청을 위한 DTO
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "챌린지 완료 요청 DTO")
public class ChallengeCompleteRequestDto {
    private Long challengeId;
    private Integer emotion;
}

package com.a508.onestep.domain.challenge.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
챌린지 등록을 위한 요청 DTO
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "챌린지 등록 요청 DTO")
public class ChallengeRequestDto {
    private Long masterChallengeId;
    private String content;
    private Integer exp;
}

package com.a508.onestep.domain.challenge.dto.response;

import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
챌린지 완료 응답을 위한 DTO
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "챌린지 완료 응답 DTO")
public class ChallengeCompleteResponseDto {
    private Long challengeId;
    private String content;
    private Integer reaction;
    private String challengeStatus;

    public static ChallengeCompleteResponseDto from(ChallengeAssignment challengeAssignment) {
        return ChallengeCompleteResponseDto.builder()
                .challengeId(challengeAssignment.getId())
                .content(challengeAssignment.getContent())
                .reaction(challengeAssignment.getEmotion())
                .challengeStatus(challengeAssignment.getChallengeStatus().name())
                .build();
    }
}

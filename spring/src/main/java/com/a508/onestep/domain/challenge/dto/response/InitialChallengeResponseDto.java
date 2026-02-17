package com.a508.onestep.domain.challenge.dto.response;

import com.a508.onestep.domain.challenge.entity.ChallengeMaster;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class InitialChallengeResponseDto {
    @Schema(description = "참조한 마스터 챌린지의 ID(PK)")
    private Long masterChallengeId;
    @Schema(description = "챌린지 내용")
    private String content;
    @Schema(description = "챌린지 완료 시 받을 보상")
    private Integer exp;

    public static InitialChallengeResponseDto from(ChallengeMaster challengeMaster) {
        return InitialChallengeResponseDto.builder()
                .masterChallengeId(challengeMaster.getId())
                .content(challengeMaster.getTitle())
                .exp(challengeMaster.getReward())
                .build();
    }
}

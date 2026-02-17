package com.a508.onestep.domain.pet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "나의 펫 조회하기 Dto")
public class PetInfoResponseDto {
    @Schema(description = "펫 고유 ID", example = "1")
    private Long petId;

    @Schema(description = "펫 캐릭터 매핑 코드", example = "PUPPY_WHITE")
    private String petCode;

    @Schema(description = "펫 닉네임", example = "토리")
    private String petNickname;

    @Schema(description = "현재 레벨에서 쌓은 경험치", example = "1000")
    private Integer currentExp;

    @Schema(description = "현재 레벨의 최대 경험치", example = "2000")
    private Integer maxExp;

    @Schema(description = "펫의 레벨", example = "11")
    private Integer level;

    @Schema(description = "누적 전체 경험치", example = "100000")
    private Integer totalExp;

    @Schema(description = "현재 레벨 구간 시작 누적 경험치", example = "740")
    private Integer levelStartExp;

    @Schema(description = "현재 레벨 구간 끝(다음 레벨 시작) 누적 경험치, 만렙이면 -1", example = "1080")
    private Integer levelEndExp;

    public static PetInfoResponseDto petInfoResponseDtoOf(
            Long petId, String petCode, String petNickname,
            Integer currentExp, Integer maxExp, Integer level, Integer totalExp,
            Integer levelStartExp, Integer levelEndExp
    ) {
        return PetInfoResponseDto.builder()
                .petId(petId)
                .petCode(petCode)
                .petNickname(petNickname)
                .currentExp(currentExp)
                .maxExp(maxExp)
                .level(level)
                .totalExp(totalExp)
                .levelStartExp(levelStartExp)
                .levelEndExp(levelEndExp)
                .build();
    }
}
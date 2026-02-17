package com.a508.onestep.domain.pet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "펫 정보 수정 요청 DTO")
public class PetUpdateRequestDto {

    @Schema(description = "변경할 펫 코드", example = "CAT_BLACK")
    private String petCode;

    @Schema(description = "펫 닉네임", example = "토리")
    private String petNickname;

    @Schema(description = "새로 획득한 경험치량", example = "40")
    private Integer addedExp;

    @Schema(description = "변경할 경험치(필요 시)", example = "5000")
    private Integer currentExp;

    @Schema(description = "변경할 레벨(필요 시)", example = "2")
    private Integer petLevel;

    @Schema(description = "메인 펫 여부", example = "true")
    private Boolean isMain;
}
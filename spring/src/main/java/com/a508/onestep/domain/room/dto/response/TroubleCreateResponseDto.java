package com.a508.onestep.domain.room.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Schema(description = "고민 생성 응답 DTO")
public class TroubleCreateResponseDto {

    @Schema(description = "생성된 고민 ID", example = "1")
    private Long troubleId;
}
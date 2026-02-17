package com.a508.onestep.domain.room.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "고민 생성 요청 DTO")
public class TroubleCreateRequestDto {

    @Schema(
            description = "고민 내용",
            example = "요즘 진로 때문에 고민이 많아요."
    )
    private String content;
}

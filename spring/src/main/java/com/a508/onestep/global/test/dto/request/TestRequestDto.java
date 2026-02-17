package com.a508.onestep.global.test.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "테스트 요청 DTO")
public class TestRequestDto {

    @Schema(description = "이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "내용", example = "테스트 내용입니다", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}

package com.a508.onestep.domain.route.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "산책 추천 조회 요청 DTO")
public class RouteLocationRequestDto {
    @Schema(description = "사용자 위도")
    private Double latitude;
    @Schema(description = "사용자 경도")
    private Double longitude;
}

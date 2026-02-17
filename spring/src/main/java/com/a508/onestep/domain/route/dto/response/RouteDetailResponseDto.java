package com.a508.onestep.domain.route.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "산책 추천 상세 조회 응답 DTO")
public class RouteDetailResponseDto {
    @Schema(description = "추천 ID(PK)")
    private Long routeId;
    @Schema(description = "내용")
    private String content;
    @Schema(description = "추천 날짜")
    private LocalDate date;
    @Schema(description = "현재 사용자 위도")
    private Double latitude;
    @Schema(description = "현재 사용자 경도")
    private Double longitude;
    @Schema(description = "예상 소요 시간")
    private Integer estimatedTime;
}
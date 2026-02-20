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
@Schema(description = "산책 추천 응답 DTO")
public class RouteRecommendResponseDto {
    @Schema(description = "추천 ID(PK)")
    private Long routeId;
    @Schema(description = "내용")
    private String content;
    @Schema(description = "추천 생성 유형 (RECOMMENDED(시스템 추천) / SELF(사용자) / ROUTE(산책 추천))")
    private String origin;
    @Schema(description = "추천 날짜")
    private LocalDate date;
    @Schema(description = "현재 완료 여부 상태")
    private String status;
}

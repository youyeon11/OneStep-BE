package com.a508.onestep.domain.challenge.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "월별 감정 기록 조회")
public class DailyEmotionResponseDto {
    // 날짜
    @Schema(description = "챌린지 수행 날짜", example = "2026-01-01", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate date;

    // 그 날짜의 챌린지 평균값(반올림)
    @Schema(description = "챌린지 감정의 평균(반올림)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private Long average;

}

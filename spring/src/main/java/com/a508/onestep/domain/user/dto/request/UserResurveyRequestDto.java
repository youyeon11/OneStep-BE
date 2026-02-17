package com.a508.onestep.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "설문 재등록 요청 DTO")
public class UserResurveyRequestDto {
    @Schema(description = "다시 질문한 설문 문항 번호")
    private Integer surveyNumber;
    @Schema(description = "재응답")
    private Integer answer;
}

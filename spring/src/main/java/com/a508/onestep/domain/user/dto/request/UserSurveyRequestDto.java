package com.a508.onestep.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "설문 결과 등록 요청 DTO")
@Builder
@AllArgsConstructor
public class UserSurveyRequestDto {

    @Schema(description = "설문 응답 리스트 (0~3점 사이의 정수 15개)", example = "[1, 2, 0, 3, 1, 1, 2, 0, 3, 2, 1, 0, 3, 2, 1]")
    private List<Integer> answers;

    /**
     * 전체 점수 합산
     */
    public int getTotalScore() {
        if (answers == null || answers.isEmpty()) {
            return 0;
        }
        return answers.stream()
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
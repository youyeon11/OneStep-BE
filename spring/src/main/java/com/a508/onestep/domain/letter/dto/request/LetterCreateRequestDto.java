package com.a508.onestep.domain.letter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "편지 작성 요청 DTO")
public class LetterCreateRequestDto {

    @Schema(description = "편지 제목", example = "안녕?")
    @Size(max = 10, message = "제목은 10자 이내여야 합니다.")
    private String title;

    @Schema(description = "편지 내용", example = "안녕? 오늘 하루는 어땠어?")
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 100, message = "내용은 100자 이내여야 합니다.")
    private String content;
}
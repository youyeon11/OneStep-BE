package com.a508.onestep.domain.letter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

// 상세조회 내용만 조회
public class SavedLetterDetailResponseDto {
    private String content;
}


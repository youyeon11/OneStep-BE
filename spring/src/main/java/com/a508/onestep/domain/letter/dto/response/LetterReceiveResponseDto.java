package com.a508.onestep.domain.letter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterReceiveResponseDto {
    private Long letterId;
    private String title;
    private String content;
    private LocalDateTime deliveredAt;
}
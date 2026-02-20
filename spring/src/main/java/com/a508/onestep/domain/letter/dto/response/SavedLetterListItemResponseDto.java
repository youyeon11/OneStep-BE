package com.a508.onestep.domain.letter.dto.response;

import com.a508.onestep.domain.letter.entity.LetterDelivery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class SavedLetterListItemResponseDto {

    private Long letterId;
    private String title;
    private LocalDate readAt;

    public static SavedLetterListItemResponseDto from(LetterDelivery d) {
        return SavedLetterListItemResponseDto.builder()
                .letterId(d.getLetter().getId())
                .title(d.getLetter().getTitle())
                .readAt(
                        d.getReadAt() != null
                                ? d.getReadAt().toLocalDate()
                                : (d.getDeliveredAt() != null ? d.getDeliveredAt().toLocalDate() : null)
                )
                .build();
    }
}

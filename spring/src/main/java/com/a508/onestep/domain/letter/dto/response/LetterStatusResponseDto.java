package com.a508.onestep.domain.letter.dto.response;

import com.a508.onestep.domain.letter.entity.StorageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterStatusResponseDto {
    private Long letterId;
    private StorageStatus storageStatus;
}

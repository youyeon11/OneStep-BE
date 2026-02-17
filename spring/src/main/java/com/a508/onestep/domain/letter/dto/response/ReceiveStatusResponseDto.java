package com.a508.onestep.domain.letter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiveStatusResponseDto {
    private boolean isOpen;
}

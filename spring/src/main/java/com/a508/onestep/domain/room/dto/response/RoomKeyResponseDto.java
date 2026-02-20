package com.a508.onestep.domain.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomKeyResponseDto {

    private Long roomId; // roomId
    private Long troubleId;

}

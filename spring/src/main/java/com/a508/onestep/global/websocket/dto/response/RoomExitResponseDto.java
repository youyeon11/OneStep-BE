package com.a508.onestep.global.websocket.dto.response;


import com.a508.onestep.domain.common.ChatEventType;
import com.a508.onestep.domain.room.entity.Room;
import com.a508.onestep.global.websocket.dto.RoomSession;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomExitResponseDto {
    private Long roomId;
    private String userCode;
    private String eventType;
    private LocalDateTime timestamp;
    private int remainingCount;

    public static RoomExitResponseDto from(Room room, RoomSession roomSession, String userCode) {
        return RoomExitResponseDto.builder()
                .roomId(room.getId())
                .userCode(userCode)
                .eventType(ChatEventType.EXIT.name())
                .timestamp(LocalDateTime.now())
                .remainingCount(roomSession.getCurrentCount())
                .build();
    }
}

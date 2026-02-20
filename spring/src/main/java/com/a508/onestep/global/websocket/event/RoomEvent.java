package com.a508.onestep.global.websocket.event;

import com.a508.onestep.domain.common.RoomEventType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomEvent {

    private final RoomEventType eventType;
    private final Long roomId;
    private final String userCode;
    private final LocalDateTime timestamp;

    public static RoomEvent enter(String userCode, Long roomId) {
        return RoomEvent.builder()
                .eventType(RoomEventType.ENTER)
                .roomId(roomId)
                .userCode(userCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static RoomEvent exit(String userCode, Long roomId) {
        return RoomEvent.builder()
                .eventType(RoomEventType.EXIT)
                .roomId(roomId)
                .userCode(userCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public boolean isEnter() {
        return eventType == RoomEventType.ENTER;
    }

    public boolean isExit() {
        return eventType == RoomEventType.EXIT;
    }
}

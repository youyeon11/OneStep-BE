package com.a508.onestep.global.websocket.event;

import com.a508.onestep.domain.common.ChatEventType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomEvent {

    private final ChatEventType eventType;
    private final Long roomId;
    private final String userCode;
    private final LocalDateTime timestamp;

    public static RoomEvent enter(String userCode, Long roomId) {
        return RoomEvent.builder()
                .eventType(ChatEventType.ENTER)
                .roomId(roomId)
                .userCode(userCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static RoomEvent exit(String userCode, Long roomId) {
        return RoomEvent.builder()
                .eventType(ChatEventType.EXIT)
                .roomId(roomId)
                .userCode(userCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public boolean isEnter() {
        return eventType == ChatEventType.ENTER;
    }

    public boolean isExit() {
        return eventType == ChatEventType.EXIT;
    }
}

package com.a508.onestep.global.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/*
Redis에 저장할 방 세션 정보
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomSession {
    private Long roomId;
    private Set<RoomParticipant> participants;
    private LocalDateTime sessionStartedAt;
    private LocalDateTime sessionExpiresAt;

    public int getCurrentCount() {
        return participants != null ? participants.size() : 0;
    }

    public boolean isFull() {
        return getCurrentCount() >= 4;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(sessionExpiresAt);
    }
}

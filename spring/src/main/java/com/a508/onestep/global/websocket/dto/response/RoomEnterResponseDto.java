package com.a508.onestep.global.websocket.dto.response;

import com.a508.onestep.domain.common.ChatEventType;
import com.a508.onestep.domain.room.entity.Room;
import com.a508.onestep.global.websocket.dto.RoomParticipant;
import com.a508.onestep.global.websocket.dto.RoomSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomEnterResponseDto {
    private Long roomId;
    private String topic;
    private Integer durationTime;
    private List<RoomParticipant> participants;
    private LocalDateTime expiresAt;
    private String userCode;
    private String eventType;
    private LocalDateTime timestamp;
    private Long remainingTime;

    public static RoomEnterResponseDto from(Room room, RoomSession roomSession, String userCode) {
        LocalDateTime now = LocalDateTime.now();
        long remainingMillis = ChronoUnit.MILLIS.between(now, roomSession.getSessionExpiresAt());

        return RoomEnterResponseDto.builder()
                .roomId(room.getId())
                .topic(room.getTopic())
                .durationTime(room.getDurationTime())
                .participants(new ArrayList<>(roomSession.getParticipants()))
                .expiresAt(roomSession.getSessionExpiresAt())
                .userCode(userCode)
                .eventType(ChatEventType.ENTER.name())
                .timestamp(now)
                .remainingTime(Math.max(0L, remainingMillis))
                .build();
    }
}

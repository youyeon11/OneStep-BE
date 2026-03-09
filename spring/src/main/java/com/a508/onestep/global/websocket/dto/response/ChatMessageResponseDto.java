package com.a508.onestep.global.websocket.dto.response;

import com.a508.onestep.domain.common.ChatEventType;
import com.a508.onestep.global.websocket.dto.RoomParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/*
WebSocket에서 채팅에 사용하는 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDto {

    private String messageId;
    private Long roomId;
    private String topic;
    private String senderCode;
    private String content;
    private LocalDateTime timestamp;
    private ChatEventType messageRoleType;
    private Integer durationTime;
    private List<RoomParticipant> participants;
    private LocalDateTime expiresAt;
    private Long remainingTime;
    private Integer currentCount;
}

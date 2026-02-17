package com.a508.onestep.global.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
Redis에 저장할 참가자 목록
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomParticipant {
    private String userCode;
    private LocalDateTime enteredAt;
}

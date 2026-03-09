package com.a508.onestep.global.websocket.dto.request;

import com.a508.onestep.domain.common.ChatEventType;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomMessageRequestDto {
    private Long roomId;
    private String senderCode;
    private String content;
    private ChatEventType messageRoleType;
}

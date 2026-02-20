package com.a508.onestep.global.websocket.dto.request;

import com.a508.onestep.domain.common.MessageRoleType;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomMessageRequestDto {
    private Long roomId;
    private String senderCode;
    private String content;
    private MessageRoleType messageRoleType;
}

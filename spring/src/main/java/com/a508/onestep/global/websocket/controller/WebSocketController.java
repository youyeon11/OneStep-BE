package com.a508.onestep.global.websocket.controller;

import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.websocket.dto.request.ChatMessageRequestDto;
import com.a508.onestep.global.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketService webSocketService;

    /**
     * 연결 테스트
     * Client: SEND /app/connect
     */
    @MessageMapping("/connect")
    public void connect() {
        webSocketService.connect();
    }

    /**
     * 방 입장
     * Client: SEND /app/room/{roomId}/enter
     * Subscribe: /user/queue/room/enter (방 정보 수신)
     * Subscribe: /topic/room/{roomId}/events (입장/퇴장/타이머 알림)
     *   - ENTER: 사용자 입장
     *   - EXIT: 사용자 퇴장
     *   - CLOSING_SOON: 종료 3분 전 알림
     *   - CLOSED: 방 종료
     * Subscribe: /topic/room/{roomId}/messages (채팅 메시지)
     */
    @MessageMapping("/room/{roomId}/enter")
    public void enterRoom(@DestinationVariable("roomId") Long roomId) {
        webSocketService.enterRoom(roomId);
    }

    /**
     * 방 퇴장
     * Client: SEND /app/room/{roomId}/exit
     */
    @MessageMapping("/room/{roomId}/exit")
    public void exitRoom(@DestinationVariable("roomId") Long roomId) {
        webSocketService.exitRoom(roomId);
    }

    /**
     * 메시지 전송
     * Client: SEND /app/room/{roomId}/message
     * Subscribe: /topic/room/{roomId}/messages
     */
    @MessageMapping("/room/{roomId}/message")
    public void sendMessage(
            @DestinationVariable("roomId") Long roomId,
            @Payload ChatMessageRequestDto request
    ) {
        webSocketService.sendMessage(roomId, request);
    }

    /**
     * 예외 처리
     */
    @MessageExceptionHandler
    public void handleException(Exception exception) {
        LogUtils.error("[WebSocket] Message handling error: {}", exception.getMessage(), exception);
    }
}

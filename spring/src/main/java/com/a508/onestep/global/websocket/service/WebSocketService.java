package com.a508.onestep.global.websocket.service;

import com.a508.onestep.global.websocket.dto.request.ChatMessageRequestDto;

public interface WebSocketService {

    void connect();

    void enterRoom(Long roomId);

    void exitRoom(Long roomId);

    void sendMessage(Long roomId, ChatMessageRequestDto requestDto);
}

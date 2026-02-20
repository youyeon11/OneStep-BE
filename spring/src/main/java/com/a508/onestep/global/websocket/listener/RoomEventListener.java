package com.a508.onestep.global.websocket.listener;

import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.websocket.event.RoomEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomEventListener {

    @Async
    @EventListener
    public void handleRoomEvent(RoomEvent event) {
        if (event.isEnter()) {
            LogUtils.info("[RoomEvent] User {} entered room {}", event.getUserCode(), event.getRoomId());
        } else if (event.isExit()) {
            LogUtils.info("[RoomEvent] User {} exited room {}", event.getUserCode(), event.getRoomId());
        }
    }
}

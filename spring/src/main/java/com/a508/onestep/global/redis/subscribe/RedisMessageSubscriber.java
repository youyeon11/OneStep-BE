package com.a508.onestep.global.redis.subscribe;

import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.message.dto.StompMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.connection.Message;

@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Topic 메시지 처리
     */
    public void handleTopicMessage(Message message, byte[] pattern) {
        handleBroadcastMessage(message, "topic");
    }

    /**
     * Queue 메시지 처리
     */
    public void handleQueueMessage(Message message, byte[] pattern) {
        handleBroadcastMessage(message, "queue");
    }

    private void handleBroadcastMessage(Message message, String channelType) {
        try {
            StompMessage stompMessage = objectMapper.readValue(message.getBody(), StompMessage.class);
            simpMessagingTemplate.convertAndSend(stompMessage.getDestination(), stompMessage.getPayload());
            LogUtils.info("Forwarded {} message to: {}", channelType, stompMessage.getDestination());
        } catch (Exception e) {
            LogUtils.error("Error handling {} message", channelType, e);
        }
    }

    /**
     * User 메시지 처리 - /user/{userCode}
     */
    public void handleUserMessage(Message message, byte[] pattern) {
        try {
            StompMessage stompMessage = objectMapper.readValue(message.getBody(), StompMessage.class);

            if (stompMessage.getUserCode() != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        stompMessage.getUserCode(),
                        stompMessage.getDestination().replace("/user/" + stompMessage.getUserCode(), ""),
                        stompMessage.getPayload()
                );
                LogUtils.info("Forwarded user message to: {}", stompMessage.getUserCode());
            }
        } catch (Exception e) {
            LogUtils.error("Error handling user message", e);
        }
    }
}

package com.a508.onestep.global.message;

import com.a508.onestep.domain.common.MessageType;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.message.dto.StompMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class StompMessageTemplate implements MessageTemplate {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topicChannel;
    private final ChannelTopic queueChannel;
    private final ChannelTopic userChannel;

    /**
     * 전송
     * @param destination 목적지
     * @param payload 전송 메시지
     */
    @Override
    public void send(String destination, Object payload) {
        send(destination, payload, null);
    }

    /**
     * 헤더와 함께 전송
     * @param destination 목적지
     * @param payload 전송할 메시지
     * @param headers 추가 헤더
     */
    @Override
    public void send(String destination, Object payload, Map<String, Object> headers) {
        try {
            MessageType type = determineMessageType(destination);

            StompMessage stompMessage = StompMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .type(type)
                    .destination(destination)
                    .payload(payload)
                    .headers(headers)
                    .build();

            publishToRedis(type, stompMessage);

            LogUtils.debug("Message sent via Redis - Type: {}, Destination: {}, MessageId: {}",
                    type, destination, stompMessage.getMessageId());

        } catch (Exception e) {
            LogUtils.error("Failed to send message to destination: {}", destination, e);
            throw BusinessException.of(ErrorCode.FAILED_SEND_MESSAGE);
        }
    }

    /**
     * 메시지 타입 결정
     */
    private MessageType determineMessageType(String destination) {
        if (destination.startsWith("/topic")) {
            return MessageType.TOPIC;
        } else if (destination.startsWith("/queue")) {
            return MessageType.QUEUE;
        } else if (destination.startsWith("/user")) {
            return MessageType.USER;
        }
        return MessageType.TOPIC;
    }

    /**
     * Redis Pub/Sub으로 메시지 발행
     */
    private void publishToRedis(MessageType type, StompMessage message) {
        ChannelTopic channel = getChannelByType(type);
        redisTemplate.convertAndSend(channel.getTopic(), message);
    }

    /**
     * 메시지 타입에 따른 채널 선택
     */
    private ChannelTopic getChannelByType(MessageType type) {
        switch (type) {
            case TOPIC:
                return topicChannel;
            case QUEUE:
                return queueChannel;
            case USER:
                return userChannel;
            default:
                return topicChannel;
        }
    }
}

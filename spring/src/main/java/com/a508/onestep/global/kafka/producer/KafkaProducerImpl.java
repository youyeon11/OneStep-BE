package com.a508.onestep.global.kafka.producer;

import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.kafka.dto.KafkaMessageDto;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
Kafka 전송 로직
 */
@RequiredArgsConstructor
@Component
public class KafkaProducerImpl implements KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /*
    단일 메시지 동기 전송
     */
    @Override
    public <T> void send(String topic, String key, KafkaMessageDto<T> messageDto) {
        try {
            // 동기 방식
            kafkaTemplate.send(topic, key, messageDto).get();
            LogUtils.debug("Message sent successfully to topic: {}, key: {}, messageId: {}",
                    topic, key, messageDto.getId());
        } catch (Exception e) {
            LogUtils.error("Failed to send message to topic: {}, key: {}, messageId: {}",
                    topic, key, messageDto.getId(), e);
            throw BusinessException.of(ErrorCode.KAFKA_SEND_FAILED);
        }
    }

    /*
    단일 메시지 비동기 전송
     */
    @Override
    public <T> CompletableFuture<Void> sendAsync(String topic, String key, KafkaMessageDto<T> messageDto) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, messageDto);

        return future.handle((result, ex) -> {
            if (ex == null) {
                LogUtils.debug("Message sent successfully to topic: {}, partition: {}, offset: {}, messageId: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        messageDto.getId());
                return null;
            } else {
                LogUtils.error("Failed to send message to topic: {}, key: {}, messageId: {}",
                        topic, key, messageDto.getId(), ex);
                throw BusinessException.of(ErrorCode.KAFKA_SEND_FAILED);
            }
        });
    }

    /*
    메시지 키 없이 전송
     */
    @Override
    public <T> void send(String topic, KafkaMessageDto<T> message) {
        send(topic, null, message);
    }

    @Override
    public <T> void sendBatch(String topic, List<KafkaMessageDto<T>> messageDtoList) {
        LogUtils.info("Sending batch messages to topic: {}, count: {}", topic, messageDtoList.size());

        int successCount = 0;
        int failCount = 0;

        for (KafkaMessageDto<T> message : messageDtoList) {
            try {
                sendAsync(topic, null, message);
                successCount++;
            } catch (Exception e) {
                failCount++;
                LogUtils.error("Failed to send batch message: messageId: {}", message.getId(), e);
            }
        }

        LogUtils.info("Batch send completed - Success: {}, Failed: {}", successCount, failCount);
    }

    /*
    단순 String 전송
     */
    @Override
    public void send(String topic, String value) {
        try {
            kafkaTemplate.send(topic, value).get();
            LogUtils.info("String message sent to topic: {}", topic);
        } catch (Exception e) {
            LogUtils.error("Failed to send string message to topic: {}", topic, e);
            throw new RuntimeException("Kafka string message send failed", e);
        }
    }
}

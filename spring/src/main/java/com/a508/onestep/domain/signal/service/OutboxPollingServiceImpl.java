package com.a508.onestep.domain.signal.service;

import com.a508.onestep.domain.signal.entity.OutboxEvent;
import com.a508.onestep.domain.signal.repository.OutboxEventRepository;
import com.a508.onestep.global.kafka.topic.KafkaTopics;
import com.a508.onestep.global.kafka.producer.KafkaProducer;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxPollingServiceImpl implements OutboxPollingService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaProducer kafkaProducer;

    @Value("${outbox.poll.batch-size:100}")
    private int batchSize;

    @Override
    @Transactional
    public int publishPendingOutboxEvents() {
        List<OutboxEvent> outboxEvents = outboxEventRepository.findPendingForUpdate(batchSize);
        if (outboxEvents.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        int failedCount = 0;

        for (OutboxEvent outboxEvent : outboxEvents) {
            try {
                kafkaProducer.send(KafkaTopics.OUTBOX_EVENT, outboxEvent.getPayload());
                outboxEvent.markSent();
                sentCount++;
            } catch (Exception e) {
                outboxEvent.markFailed(trimErrorMessage(e.getMessage()));
                failedCount++;
                LogUtils.error("Failed to publish outbox event: outboxId={}", outboxEvent.getId(), e);
            }
        }

        LogUtils.info("Outbox polling completed: total={}, sent={}, failed={}",
                outboxEvents.size(), sentCount, failedCount);
        return sentCount;
    }

    private String trimErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}

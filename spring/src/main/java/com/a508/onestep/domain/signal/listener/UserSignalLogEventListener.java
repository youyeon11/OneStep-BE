package com.a508.onestep.domain.signal.listener;

import com.a508.onestep.domain.common.AggregateType;
import com.a508.onestep.domain.signal.entity.OutboxEvent;
import com.a508.onestep.domain.signal.entity.UserSignalLog;
import com.a508.onestep.domain.signal.event.UserSignalLogEvent;
import com.a508.onestep.domain.signal.repository.OutboxEventRepository;
import com.a508.onestep.domain.signal.repository.UserSignalLogRepository;
import com.a508.onestep.global.kafka.topic.KafkaTopics;
import com.a508.onestep.global.logging.utils.LogUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserSignalLogEventListener {

    private static final String OUTBOX_EVENT_TYPE = "USER_SIGNAL_LOG_EVENT";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserSignalLogRepository userSignalLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserSignalLogEvent(UserSignalLogEvent event) {
        LogUtils.info(
                "Handling UserSignalLogEvent: userCode={}, targetId={}, eventType={}",
                event.getUserCode(),
                event.getTargetId(),
                event.getEventType()
        );

        try {
            UserSignalLog userSignalLog = UserSignalLog.builder()
                    .userCode(event.getUserCode())
                    .targetId(event.getTargetId())
                    .eventStatus(event.getEventStatus())
                    .eventType(event.getEventType())
                    .metadata(buildMetadata(event))
                    .build();
            userSignalLogRepository.save(userSignalLog);

            // OutboxEvent 생성 및 저장
            OutboxEvent outboxEvent = OutboxEvent.create(
                    AggregateType.CHALLENGE,
                    event.getTargetId(),
                    OUTBOX_EVENT_TYPE,
                    buildOutboxPayload(event)
            );
            outboxEventRepository.save(outboxEvent);

            LogUtils.info(
                    "Saved UserSignalLog and OutboxEvent: userCode={}, targetId={}, outboxEventType={}",
                    event.getUserCode(),
                    event.getTargetId(),
                    OUTBOX_EVENT_TYPE
            );
        } catch (Exception e) {
            LogUtils.error(
                    "Failed to save UserSignalLog/OutboxEvent: userCode={}, targetId={}, eventType={}",
                    event.getUserCode(),
                    event.getTargetId(),
                    event.getEventType(),
                    e
            );
        }
    }

    private String buildMetadata(UserSignalLogEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("assignedDate", event.getAssignedDate().format(DATE_FORMATTER));
        if (event.getCompletedAt() != null) {
            metadata.put("completedAt", event.getCompletedAt().format(DATETIME_FORMATTER));
        }
        if (event.getEmotion() != null) {
            metadata.put("emotion", event.getEmotion());
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            LogUtils.error("Failed to serialize metadata: targetId={}", event.getTargetId(), e);
            return null;
        }
    }

    private String buildOutboxPayload(UserSignalLogEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("topic", KafkaTopics.OUTBOX_EVENT);
        payload.put("key", event.getUserCode());
        payload.put("userCode", event.getUserCode());
        payload.put("targetId", event.getTargetId());
        payload.put("eventType", event.getEventType().name());
        payload.put("eventStatus", event.getEventStatus().name());
        payload.put("assignedDate", event.getAssignedDate().format(DATE_FORMATTER));
        payload.put("generatedAt", event.getGeneratedAt().format(DATETIME_FORMATTER));
        payload.put("metadata", buildMetadata(event));
        if (event.getCompletedAt() != null) {
            payload.put("completedAt", event.getCompletedAt().format(DATETIME_FORMATTER));
        }
        if (event.getEmotion() != null) {
            payload.put("emotion", event.getEmotion());
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            LogUtils.error("Failed to serialize outbox payload: targetId={}", event.getTargetId(), e);
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}

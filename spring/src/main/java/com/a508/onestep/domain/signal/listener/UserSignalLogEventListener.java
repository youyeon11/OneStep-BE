package com.a508.onestep.domain.signal.listener;

import com.a508.onestep.domain.signal.entity.UserSignalLog;
import com.a508.onestep.domain.signal.event.UserSignalLogEvent;
import com.a508.onestep.domain.signal.repository.UserSignalLogRepository;
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

    private final UserSignalLogRepository userSignalLogRepository;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserSignalLogEvent(UserSignalLogEvent event) {
        LogUtils.info("UserSignalLog 저장 : userCode = {}, targetId = {}, eventType = {}",
                event.getUserCode(), event.getTargetId(), event.getEventType()
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
            LogUtils.info("저장 성공");
        } catch (Exception e) {
            LogUtils.error("UserSignalLog 저장 실패 : userCode = {}, targetId = {}, eventType = {}",
                    event.getUserCode(), event.getTargetId(), event.getEventType()
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
            LogUtils.error("metadata 직렬화 실패 : challengeId = {}", event.getTargetId());
            return null;
        }
    }
}

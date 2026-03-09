package com.a508.onestep.global.scheduler.job;

import com.a508.onestep.domain.signal.service.OutboxPollingService;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublishJob {

    private final OutboxPollingService outboxPollingService;

    @Scheduled(
            fixedDelayString = "${outbox.poll.fixed-delay-ms:5000}",
            initialDelayString = "${outbox.poll.initial-delay-ms:5000}"
    )
    public void publishPendingOutboxEvents() {
        int publishedCount = outboxPollingService.publishPendingOutboxEvents();
        if (publishedCount > 0) {
            LogUtils.info("Published outbox events: count={}", publishedCount);
        }
    }
}

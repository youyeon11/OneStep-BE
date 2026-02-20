package com.a508.onestep.domain.signal.listener;

import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.signal.event.UserSignalLogEvent;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
동기 처리를 비동기로 변환하여 발행
 */
@Component
@RequiredArgsConstructor
public class UserSignalEventBridge {

    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChallengeCompleted(ChallengeCompletedEvent event) {
        UserSignalLogEvent signalEvent = UserSignalLogEvent.from(event);
        LogUtils.debug("UserSignalEvent 발행 시작");
        eventPublisher.publishEvent(signalEvent);
    }
}

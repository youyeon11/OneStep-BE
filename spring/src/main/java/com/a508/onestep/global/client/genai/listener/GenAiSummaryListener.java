package com.a508.onestep.global.client.genai.listener;

import com.a508.onestep.domain.room.entity.Solution;
import com.a508.onestep.domain.room.repository.SolutionRepository;
import com.a508.onestep.global.client.genai.event.GenAiSummaryEvent;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GenAiSummaryListener {

    private final SolutionRepository solutionRepository;

    /**
     * Solution 저장 이벤트 리스너
     * - 비동기로 처리하여 메인 흐름에 영향 없음
     * - 별도 트랜잭션으로 Solution 저장
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleGenAiSummaryEvent(GenAiSummaryEvent event) {
        Solution solution = Solution.builder()
                .summary(event.getSummary())
                .troubleId(event.getTroubleId())
                .build();
        solutionRepository.save(solution);
        LogUtils.info("Saved solution for roomId={}, troubleId={}", event.getRoomId(), event.getTroubleId());
    }
}

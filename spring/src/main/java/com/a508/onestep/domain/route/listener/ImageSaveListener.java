package com.a508.onestep.domain.route.listener;

import com.a508.onestep.domain.route.event.ImageSaveEvent;
import com.a508.onestep.domain.route.repository.RouteSessionRepository;
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
public class ImageSaveListener {

    private final RouteSessionRepository routeSessionRepository;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleImageUrl(ImageSaveEvent event) {
        LogUtils.info("{}의 이미지 URL 저장 시작...", event.getRouteSessionId());

        routeSessionRepository.findById(event.getRouteSessionId())
                .ifPresentOrElse(
                        routeSession -> {
                            routeSession.saveUrl(event.getImageUrl());
                            routeSessionRepository.save(routeSession);
                            LogUtils.info("{}의 이미지 URL 저장 완료", event.getRouteSessionId());
                        },
                        () -> LogUtils.error("RouteSession을 찾을 수 없습니다. routeSessionId = {}", event.getRouteSessionId())
                );
    }
}

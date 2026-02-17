package com.a508.onestep.global.scheduler.job;

import com.a508.onestep.global.scheduler.service.UserFeatureBatchService;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class UserFeaturePublishJob {

    private final UserFeatureBatchService userFeatureBatchService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
    //@Scheduled(cron = "0 * * * * *")
    public void publishDailyUserFeatures() {
        String startTime = LocalDateTime.now().format(FORMATTER);
        LogUtils.info("====Daily UserFeature Publish Job Started====");
        LogUtils.info("startTime : {}", startTime);

        try {
            Integer publishedCount = userFeatureBatchService.publishAllUserFeatures();

            String endTime = LocalDateTime.now().format(FORMATTER);

            LogUtils.info("Published Count : {}", publishedCount);
            LogUtils.info("EndTime : {}", endTime);
        } catch (Exception e) {
            LogUtils.error("Error: {}", e.getMessage());
        }
    }
}

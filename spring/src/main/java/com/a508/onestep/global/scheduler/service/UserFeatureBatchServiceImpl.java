package com.a508.onestep.global.scheduler.service;

import com.a508.onestep.domain.signal.entity.UserSignalLog;
import com.a508.onestep.domain.signal.repository.UserSignalLogRepository;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.kafka.dto.KafkaMessageDto;
import com.a508.onestep.global.kafka.dto.UserFeatureSet;
import com.a508.onestep.global.kafka.producer.KafkaProducer;
import com.a508.onestep.global.kafka.topic.KafkaTopics;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 스케줄러에서 호출하기 위한 서비스 코드
 * UserSignalLog를 기반으로 UserFeatureSet을 생성하여 Kafka로 배치 발행
 */
@Service
@RequiredArgsConstructor
public class UserFeatureBatchServiceImpl implements UserFeatureBatchService {

    private final UserRepository userRepository;
    private final UserSignalLogRepository userSignalLogRepository;
    private final UserFeatureAggregator userFeatureAggregator;
    private final KafkaProducer kafkaProducer;

    private static final int BATCH_SIZE = 100;
    private static final String MESSAGE_TYPE = "ROUTINE_GENERATE_REQUEST";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    @Transactional(readOnly = true)
    public Integer publishAllUserFeatures() {
        LogUtils.info("========== UserFeature 배치 발행 시작 ==========");

        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 어제 하루에 대하여 시간 설정
        LocalDate yesterday = LocalDate.now(KST).minusDays(1);
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(LocalTime.MAX);

        LogUtils.info("조회 기간: {} ~ {}", startOfDay, endOfDay);

        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            Page<String> userCodesPage = userRepository.findActiveUserCodes(pageable);

            if (userCodesPage.isEmpty()) {
                hasMore = false;
                break;
            }

            List<String> userCodeList = userCodesPage.getContent();
            LogUtils.info("Batch {}: {} 명의 사용자 처리 중...", page + 1, userCodeList.size());

            // userCodeList 기반으로 한 번에 로드
            Map<String, List<UserSignalLog>> signalLogsByUser = loadSignalLogsBatch(userCodeList, startOfDay, endOfDay);

            Map<String, User> usersByCode = loadUsersBatch(userCodeList);

            // 각 유저에 대해 비동기로 처리
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String userCode : userCodeList) {
                List<UserSignalLog> userLogs = signalLogsByUser.getOrDefault(userCode, List.of());
                User user = usersByCode.get(userCode);

                CompletableFuture<Void> future = publishUserFeature(userCode, userLogs, user)
                        .thenAccept(v -> successCount.incrementAndGet())
                        .exceptionally(ex -> {
                            failureCount.incrementAndGet();
                            LogUtils.error("UserFeature 발행 실패 - userCode: {}, error: {}", userCode, ex.getMessage());
                            return null;
                        });

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            page++;
            hasMore = userCodesPage.hasNext();

            if (hasMore) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LogUtils.warn("배치 대기 중 인터럽트 발생");
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        LogUtils.info("========== UserFeature 배치 발행 완료 ==========");
        LogUtils.info("성공: {} 건", successCount.get());
        LogUtils.info("실패: {} 건", failureCount.get());
        LogUtils.info("소요 시간: {} ms ({} 초)", duration, duration / 1000.0);

        return successCount.get();
    }

    /**
     * 여러 유저의 시그널 로그를 한 번에 조회하고 userCode별로 그룹화
     */
    private Map<String, List<UserSignalLog>> loadSignalLogsBatch(
            List<String> userCodes,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    ) {
        List<UserSignalLog> allLogs = userSignalLogRepository.findByUserCodeInAndCreatedAtBetween(
                userCodes, startOfDay, endOfDay
        );

        return allLogs.stream()
                .collect(Collectors.groupingBy(UserSignalLog::getUserCode));
    }

    /**
     * 여러 유저 정보를 한 번에 조회하고 userCode별로 매핑
     */
    private Map<String, User> loadUsersBatch(List<String> userCodes) {
        List<User> users = userRepository.findByUserCodeIn(userCodes);

        return users.stream()
                .collect(Collectors.toMap(User::getUserCode, Function.identity()));
    }

    /**
     * 개별 유저에 대한 UserFeature 발행 (사전 로드된 데이터 활용)
     *
     * @param userCode   유저 코드
     * @param signalLogs 해당 유저의 시그널 로그
     * @param user       유저 엔티티
     */
    private CompletableFuture<Void> publishUserFeature(
            String userCode,
            List<UserSignalLog> signalLogs,
            User user
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                // UserFeatureAggregator를 통해 시그널 로그 → UserFeatureSet 변환
                UserFeatureSet featureSet;
                if (signalLogs.isEmpty()) {
                    featureSet = userFeatureAggregator.aggregateEmpty(userCode, user);
                } else {
                    featureSet = userFeatureAggregator.aggregate(userCode, signalLogs, user);
                }

                String messageId = userCode + "_" + LocalDate.now(KST);

                kafkaProducer.send(KafkaTopics.USER_ROUTINE_GENERATE, userCode,
                        KafkaMessageDto.<UserFeatureSet>builder()
                                .id(messageId)
                                .type(MESSAGE_TYPE)
                                .payload(featureSet)
                                .timestamp(Instant.now().toEpochMilli())
                                .build());

                LogUtils.info("UserFeature 발행 성공 - userCode: {}, signalLogs: {} 건",
                        userCode, signalLogs.size());
                if (signalLogs.size() > 0) {
                    // Debug
                    LogUtils.info(signalLogs.toString());
                }

            } catch (Exception e) {
                LogUtils.error("UserFeature 발행 실패 - userCode: {}", userCode, e);
                throw new RuntimeException(e);
            }
        });
    }
}

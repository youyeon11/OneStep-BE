package com.a508.onestep.global.scheduler.service;

import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import com.a508.onestep.domain.challenge.repository.ChallengeAssignmentRepository;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.route.entity.RouteSession;
import com.a508.onestep.domain.route.repository.RouteSessionRepository;
import com.a508.onestep.domain.signal.entity.UserSignalLog;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.global.kafka.dto.UserFeatureSet;
import com.a508.onestep.global.kafka.dto.UserFeatureSet.ChallengeHistory;
import com.a508.onestep.global.kafka.dto.UserFeatureSet.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UserSignalLog 데이터를 UserFeatureSet으로 집계하는 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class UserFeatureAggregator {

    private final ChallengeAssignmentRepository challengeAssignmentRepository;
    private final RouteSessionRepository routeSessionRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public UserFeatureSet aggregate(String userCode, List<UserSignalLog> signalLogs, User user) {
        List<ChallengeHistory> challengeHistories = new ArrayList<>();
        UserFeatureSet.RouteInfo routeInfo = null;

        // targetId 수집
        Set<Long> challengeIds = new HashSet<>();
        Set<Long> routeSessionIds = new HashSet<>();

        for (UserSignalLog log : signalLogs) {
            if (log.getEventType() == Origin.ROUTE) {
                routeSessionIds.add(log.getTargetId());
            } else {
                challengeIds.add(log.getTargetId());
            }
        }

        // 일괄 조회
        Map<Long, ChallengeAssignment> challengeMap = challengeAssignmentRepository
                .findAllById(challengeIds)
                .stream()
                .collect(Collectors.toMap(ChallengeAssignment::getId, Function.identity()));

        Map<Long, RouteSession> routeSessionMap = routeSessionRepository
                .findAllById(routeSessionIds)
                .stream()
                .collect(Collectors.toMap(RouteSession::getId, Function.identity()));

        // 로그 처리
        for (UserSignalLog log : signalLogs) {
            if (log.getEventType() == Origin.ROUTE) {
                RouteSession routeSession = routeSessionMap.get(log.getTargetId());
                if (routeSession != null) {
                    routeInfo = buildRouteInfo(log, routeSession);
                }
            } else {
                ChallengeAssignment assignment = challengeMap.get(log.getTargetId());
                if (assignment != null) {
                    challengeHistories.add(buildChallengeHistory(log, assignment));
                }
            }
        }

        UserContext userContext = buildUserContext(user);

        return UserFeatureSet.builder()
                .userCode(userCode)
                .requestDate(LocalDate.now().format(DATE_FORMATTER))
                .userContext(userContext)
                .challengeHistory(challengeHistories)
                .routeInfo(routeInfo)
                .build();
    }

    public UserFeatureSet aggregateEmpty(String userCode, User user) {
        UserContext userContext = buildUserContext(user);

        return UserFeatureSet.builder()
                .userCode(userCode)
                .requestDate(LocalDate.now().format(DATE_FORMATTER))
                .userContext(userContext)
                .challengeHistory(List.of())
                .routeInfo(null)
                .build();
    }

    private ChallengeHistory buildChallengeHistory(UserSignalLog log, ChallengeAssignment assignment) {
        String assignedDate = assignment.getAssignedDate() != null
                ? assignment.getAssignedDate().format(DATE_FORMATTER)
                : null;

        String completedAt = assignment.getLogDate() != null
                ? assignment.getLogDate().format(DATE_FORMATTER)
                : null;

        return ChallengeHistory.builder()
                .challengeId(assignment.getId())
                .challengeMasterId(assignment.getChallengeMasterId())
                .challengeStatus(log.getEventStatus() != null ? log.getEventStatus().name() : null)
                .assignedDate(assignedDate)
                .completedAt(completedAt)
                .emotion(assignment.getEmotion())
                .origin(log.getEventType() != null ? log.getEventType().name() : null)
                .weight(log.getWeight())
                .build();
    }

    private UserContext buildUserContext(User user) {
        if (user == null) {
            return UserContext.builder()
                    .recoveryLevel(null)
                    .build();
        }

        return UserContext.builder()
                .recoveryLevel(user.getRecoveryLevel())
                .build();
    }

    private UserFeatureSet.RouteInfo buildRouteInfo(UserSignalLog log, RouteSession routeSession) {
        String completedAt = routeSession.getEndedAt() != null
                ? routeSession.getEndedAt().toString()
                : null;

        return UserFeatureSet.RouteInfo.builder()
                .routeSessionId(routeSession.getId())
                .content(routeSession.getContent())
                .completedAt(completedAt)
                .challengeStatus(routeSession.getRouteStatus() != null ? routeSession.getRouteStatus().name() : null)
                .origin(log.getEventType() != null ? log.getEventType().name() : null)
                .build();
    }
}

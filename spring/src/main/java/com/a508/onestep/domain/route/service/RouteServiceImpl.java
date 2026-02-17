package com.a508.onestep.domain.route.service;

import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.common.PlaceCategory;
import com.a508.onestep.domain.route.dto.request.RouteLocationRequestDto;
import com.a508.onestep.domain.route.dto.response.RouteChallengeResponseDto;

import com.a508.onestep.domain.route.dto.response.RouteDetailResponseDto;
import com.a508.onestep.domain.route.dto.response.RouteRecommendResponseDto;
import com.a508.onestep.domain.route.entity.RouteLevel;
import com.a508.onestep.domain.route.entity.RouteSession;
import com.a508.onestep.domain.route.repository.RouteLevelRepository;
import com.a508.onestep.domain.route.repository.RouteSessionRepository;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.client.kakao.KakaoApiClient;
import com.a508.onestep.global.client.kakao.dto.KakaoMapResponseDto;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private static final int DEFAULT_PLACE_COUNT = 10;
    private static final String GRID_FORMAT = "%.6f,%.6f";
    private static final double ARRIVAL_THRESHOLD_METERS = 300.0;

    private final UserRepository userRepository;
    private final RouteLevelRepository routeLevelRepository;
    private final RouteSessionRepository routeSessionRepository;
    private final KakaoApiClient kakaoApiClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 사용자 위치 기반 경로 추천
     */
    @Override
    @Transactional
    public RouteRecommendResponseDto getRecommendation(RouteLocationRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        LogUtils.info("경로 추천 요청 - 사용자: {}, 위도: {}, 경도: {}",
                userCode, requestDto.getLatitude(), requestDto.getLongitude());

        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        // 이미 오늘 하루 존재하는지 확인
        LocalDate today = LocalDate.now();
        Optional<RouteSession> existingRouteSession =
                routeSessionRepository.findByUserIdAndLogDate(user.getId(), today);

        if (existingRouteSession.isPresent()) {
            RouteSession session = existingRouteSession.get();

            return RouteRecommendResponseDto.builder()
                    .routeId(session.getId())
                    .content(session.getContent())
                    .origin(Origin.ROUTE.name())
                    .date(today)
                    .status(session.getRouteStatus().name())
                    .build();
        }

        RouteLevel routeLevel = routeLevelRepository.findByRouteLevel(user.getRecoveryLevel())
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROUTE_LEVEL_NOT_FOUND));

        PlaceCategory[] categories = PlaceCategory.values();
        PlaceCategory selectedCategory = categories[ThreadLocalRandom.current().nextInt(categories.length)];

        LogUtils.debug("추천 파라미터 - 반경: {}m, 카테고리: {}",
                routeLevel.getRecommendedMaxDistanceM(), selectedCategory.getDescription());

        KakaoMapResponseDto mapResponse = kakaoApiClient.searchPlacesByCategory(
                requestDto.getLatitude(),
                requestDto.getLongitude(),
                routeLevel.getRecommendedMaxDistanceM(),
                DEFAULT_PLACE_COUNT,
                selectedCategory);

        if (mapResponse == null || mapResponse.getDocuments() == null || mapResponse.getDocuments().isEmpty()) {
            LogUtils.warn("검색 결과 없음 - 사용자: {}, 카테고리: {}",
                    user.getUserCode(), selectedCategory.getDescription());
            return null;
        }

        var documents = mapResponse.getDocuments();
        var selectedPlace = documents.get(ThreadLocalRandom.current().nextInt(documents.size()));

        String destinationGrid = String.format(
                GRID_FORMAT,
                Double.parseDouble(selectedPlace.getY()), // latitude
                Double.parseDouble(selectedPlace.getX())  // longitude
        );

        String savedContent = buildRecommendationContent(selectedPlace);
        RouteSession savedSession = routeSessionRepository.save(RouteSession.builder()
                .user(user)
                .routeStatus(AssignmentStatus.ASSIGNED)
                .routeLevel(routeLevel)
                .logDate(today)
                .content(savedContent)
                .destinationGrid(destinationGrid)
                .build());

        LogUtils.info("경로 추천 완료 - 사용자: {}, 경로 ID: {}", userCode, savedSession.getId());

        return RouteRecommendResponseDto.builder()
                .routeId(savedSession.getId())
                .content(savedContent)
                .origin(Origin.ROUTE.name())
                .date(today)
                .status(AssignmentStatus.ASSIGNED.name())
                .build();
    }

    /**
     * 멘트 생성
     */
    private String buildRecommendationContent(KakaoMapResponseDto.Document place) {
        StringBuilder content = new StringBuilder()
                .append(place.getPlaceName()).append("을(를) 방문해보세요.");

        if (place.getDistance() != null && !place.getDistance().isEmpty()) {
            content.append(" (거리: ").append(place.getDistance()).append("m)");
        }

        String address = place.getRoadAddressName() != null && !place.getRoadAddressName().isEmpty()
                ? place.getRoadAddressName()
                : place.getAddressName();
        if (address != null && !address.isEmpty()) {
            content.append(" 주소: ").append(address);
        }

        return content.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public RouteDetailResponseDto getDetailRoute(Long routeId, RouteLocationRequestDto requestDto) {

        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode).orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));
        RouteSession routeSession =  routeSessionRepository.findById(routeId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROUTE_SESSION_NOT_FOUND));
        if (!routeSession.getUser().equals(user)) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }

        // 출발지와 목적지 좌표 추출
        String startGrid = requestDto.getLatitude() + "," + requestDto.getLongitude();
        String destinationGrid = routeSession.getDestinationGrid();

        // 예상 시간 계산
        int estimatedTime = calculateEstimatedTime(startGrid, destinationGrid);
        String[] grid = routeSession.getDestinationGrid().split(",");
        Double latitude = Double.parseDouble(grid[0].trim());
        Double longitude = Double.parseDouble(grid[1].trim());

        return RouteDetailResponseDto.builder()
                .routeId(routeSession.getId())
                .content(routeSession.getContent())
                .date(routeSession.getLogDate())
                .latitude(latitude)
                .longitude(longitude)
                .estimatedTime(estimatedTime)
                .build();
    }

    /**
     * 두 지점 간 예상 소요 시간 계산 (분 단위)
     * 평균 보행 속도 4km/h 기준
     */
    private int calculateEstimatedTime(String startGrid, String endGrid) {
        String[] startCoords = startGrid.split(",");
        String[] endCoords = endGrid.split(",");

        double startLat = Double.parseDouble(startCoords[0]);
        double startLon = Double.parseDouble(startCoords[1]);
        double endLat = Double.parseDouble(endCoords[0]);
        double endLon = Double.parseDouble(endCoords[1]);

        // Haversine 공식으로 거리 계산 (미터)
        double distance = calculateDistance(startLat, startLon, endLat, endLon);

        // 평균 보행 속도 4km/h (약 67m/min)
        final double WALKING_SPEED_M_PER_MIN = 67.0;

        // 소요 시간 계산 (올림)
        int estimatedMinutes = (int) Math.ceil(distance / WALKING_SPEED_M_PER_MIN);

        LogUtils.debug("예상 시간 계산 - 거리: {}m, 소요 시간: {}분", distance, estimatedMinutes);

        return estimatedMinutes;
    }

    /**
     * Haversine 공식을 이용한 두 좌표 간 거리 계산 (미터)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c * 1000; // km를 m로 변환
    }

    /**
     * 산책 챌린지 시작
     */
    @Override
    @Transactional
    public RouteChallengeResponseDto start(Long routeId) {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        LogUtils.info("[RouteStart] 시작 요청 - userCode: {}, routeId: {}", userCode, routeId);

        RouteSession routeSession = routeSessionRepository.findById(routeId)
                .orElseThrow(() -> {
                    LogUtils.error("해당 세션을 발견할 수 없습니다.");
                    return BusinessException.of(ErrorCode.ROUTE_SESSION_NOT_FOUND);
                });
        if (!routeSession.getUser().getId().equals(user.getId())) {
            LogUtils.error("인증되지 않은 접근입니다. userCode = {}", userCode);
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }

        if (routeSession.getRouteStatus() == AssignmentStatus.COMPLETED) {
            LogUtils.error("[RouteStart] 이미 완료된 챌린지 - routeId: {}", routeId);
            throw BusinessException.of(ErrorCode.ROUTE_ALREADY_COMPLETED);
        }

        if (routeSession.getRouteStatus() == AssignmentStatus.PROGRESS) {
            LogUtils.error("[RouteStart] 이미 진행 중인 챌린지 - routeId: {}", routeId);
            throw BusinessException.of(ErrorCode.ROUTE_ALREADY_COMPLETED);
        }

        String activeKey = String.format("challenge:active:%s", userCode);
        Object existingSessionId = redisTemplate.opsForValue().get(activeKey);
        if (existingSessionId != null) {
            LogUtils.error("[RouteStart] 활성화된 다른 세션이 이미 존재함 - userCode: {}, activeSessionId: {}", userCode, existingSessionId);
            throw BusinessException.of(ErrorCode.ROUTE_ALREADY_EXISTS);
        }

        String challengeSessionId = UUID.randomUUID().toString().substring(8);
        LocalDateTime startedAt = LocalDateTime.now();

        LogUtils.info("[RouteStart] Redis 세션 생성 시작 - challengeSessionId: {}", challengeSessionId);
        redisTemplate.opsForValue().set(activeKey, challengeSessionId, Duration.ofHours(1));

        String detailKey = String.format("challenge:session:%s", challengeSessionId);

        String[] grid = routeSession.getDestinationGrid().split(",");
        String latitude = grid[0].trim();
        String longitude = grid[1].trim();

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userCode", userCode);
        sessionData.put("routeId", routeSession.getId().toString());
        sessionData.put("startedAt", startedAt.toString());
        sessionData.put("destinationX", longitude);
        sessionData.put("destinationY", latitude);

        redisTemplate.opsForHash().putAll(detailKey, sessionData);
        redisTemplate.expire(detailKey, Duration.ofHours(1));

        routeSession.start();
        LogUtils.info("[RouteStart] 챌린지 시작 완료 - userCode: {}, challengeSessionId: {}", userCode, challengeSessionId);
        return RouteChallengeResponseDto.builder()
                .routeSessionId(routeSession.getId())
                .challengeSessionId(challengeSessionId)
                .assignmentStatus(AssignmentStatus.PROGRESS.name())
                .startedAt(startedAt)
                .build();
    }

    /**
     * 진행 중인 산책 세션 조회
     */
    @Override
    public RouteChallengeResponseDto check(Long routeId) {
        String userCode = UserContextHolder.getUserCode();

        String activeKey = String.format("challenge:active:%s", userCode);
        Object challengeSessionId = redisTemplate.opsForValue().get(activeKey);
        if (challengeSessionId == null) {
            LogUtils.info("[RouteCheck] 진행 중인 세션 없음 - userCode: {}", userCode);
            return null;
        }

        String detailKey = String.format("challenge:session:%s", challengeSessionId);
        Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(detailKey);
        if (sessionData.isEmpty()) {
            LogUtils.error("[RouteCheck] 활성 키는 존재하나 상세 데이터가 없음 - challengeSessionId: {}", challengeSessionId);
            return null;
        }

        LocalDateTime startedAt = LocalDateTime.parse((String) sessionData.get("startedAt"));

        return RouteChallengeResponseDto.builder()
                .challengeSessionId((String) challengeSessionId)
                .routeSessionId(routeId)
                .assignmentStatus(AssignmentStatus.PROGRESS.name())
                .startedAt(startedAt)
                .build();
    }

    /**
     * 산책 챌린지 종료
     */
    @Override
    @Transactional
    public RouteChallengeResponseDto end(Long routeId, RouteLocationRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        LogUtils.info("[RouteEnd] 종료 요청 - userCode: {}, routeId: {}", userCode, routeId);

        // 활성 챌린지 확인
        String activeKey = String.format("challenge:active:%s", userCode);
        Object challengeSessionId = redisTemplate.opsForValue().get(activeKey);

        if (challengeSessionId == null) {
            LogUtils.error("해당 유저의 활성화된 세션을 조회할 수 없습니다. userCode = {}", userCode);
            throw BusinessException.of(ErrorCode.ROUTE_SESSION_NOT_FOUND);
        }

        String detailKey = String.format("challenge:session:%s", challengeSessionId);
        Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(detailKey);

        if (sessionData.isEmpty()) {
            LogUtils.error("해당 루트 세션을 Redis에서 발견할 수 없습니다. challengeSessionId = {}", challengeSessionId);
            throw BusinessException.of(ErrorCode.ROUTE_SESSION_NOT_FOUND);
        }

        // Redis 조회
        LocalDateTime startedAt =
                LocalDateTime.parse(((String) sessionData.get("startedAt")).trim());

        double destinationLongitude =
                Double.parseDouble(((String) sessionData.get("destinationX")).trim());
        double destinationLatitude =
                Double.parseDouble(((String) sessionData.get("destinationY")).trim());

        double currentLatitude = requestDto.getLatitude();
        double currentLongitude = requestDto.getLongitude();

        LogUtils.info("[RouteEnd] current=({}, {}), destination=({}, {})",
                currentLatitude, currentLongitude,
                destinationLatitude, destinationLongitude
        );

        double distance = calculateDistance(
                currentLatitude,
                currentLongitude,
                destinationLatitude,
                destinationLongitude
        );

        boolean isArrived = distance <= ARRIVAL_THRESHOLD_METERS;
        LogUtils.info("[RouteEnd] 거리 계산 결과 - userCode: {}, distance: {}m, threshold: {}m, isArrived: {}",
                userCode, Math.round(distance), ARRIVAL_THRESHOLD_METERS, isArrived);

        // DB 세션 조회
        RouteSession routeSession = routeSessionRepository.findById(routeId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROUTE_SESSION_NOT_FOUND));

        AssignmentStatus finalStatus;
        if (isArrived) {
            routeSession.complete();
            finalStatus = AssignmentStatus.COMPLETED;
            LogUtils.info("routeId {} 챌린지 성공", routeId);

            // 완료 이벤트 발행
            ChallengeCompletedEvent event =
                    ChallengeCompletedEvent.fromRouteSession(routeSession);
            eventPublisher.publishEvent(event);
        } else {
            routeSession.cancel();
            finalStatus = AssignmentStatus.CANCELED;
            LogUtils.info("routeId {} 챌린지 성공 실패", routeId);
            LogUtils.info("[RouteEnd] 챌린지 실패(거리 미달) - routeId: {}, userCode: {}, distance: {}m", routeId, userCode, distance);
        }

        redisTemplate.delete(activeKey);
        redisTemplate.delete(detailKey);

        LogUtils.info("[RouteEnd] Redis 세션 정리 완료 - challengeSessionId: {}", challengeSessionId);
        return RouteChallengeResponseDto.builder()
                .challengeSessionId(challengeSessionId.toString())
                .routeSessionId(routeId)
                .assignmentStatus(finalStatus.name())
                .startedAt(startedAt)
                .build();
    }
}

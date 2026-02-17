package com.a508.onestep.domain.route.service;

import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.route.dto.request.RouteLocationRequestDto;
import com.a508.onestep.domain.route.dto.response.RouteChallengeResponseDto;
import com.a508.onestep.domain.route.dto.response.RouteDetailResponseDto;
import com.a508.onestep.domain.route.dto.response.RouteRecommendResponseDto;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.PlaceCategory;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.domain.route.entity.RouteLevel;
import com.a508.onestep.domain.route.entity.RouteSession;
import com.a508.onestep.domain.route.repository.RouteLevelRepository;
import com.a508.onestep.domain.route.repository.RouteSessionRepository;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.client.kakao.KakaoApiClient;
import com.a508.onestep.global.client.kakao.dto.KakaoMapResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RouteLevelRepository routeLevelRepository;
    @Mock
    private RouteSessionRepository routeSessionRepository;
    @Mock
    private KakaoApiClient kakaoApiClient;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private RouteServiceImpl routeService;

    private User testUser;
    private RouteLevel testRouteLevel;
    private RouteLocationRequestDto requestDto;
    private KakaoMapResponseDto mockMapResponse;
    private LocalDate today;
    private String userCode;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        userCode = "TEST_USER_001";

        testUser = User.builder()
                .id(1L)
                .userCode(userCode)
                .recoveryLevel(3)
                .build();

        testRouteLevel = RouteLevel.builder()
                .id(1L)
                .routeLevel(3)
                .recommendedMaxDistanceM(1000)
                .build();

        requestDto = RouteLocationRequestDto.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        // Kakao Map Mock
        KakaoMapResponseDto.Document mockPlace = KakaoMapResponseDto.Document.builder()
                .placeName("테스트 카페")
                .x("126.9780")
                .y("37.5665")
                .distance("500")
                .roadAddressName("서울시 중구 세종대로 110")
                .addressName("서울시 중구 태평로1가 31")
                .build();

        mockMapResponse = KakaoMapResponseDto.builder()
                .documents(Arrays.asList(mockPlace))
                .build();
    }

    @Test
    @DisplayName("새로운 경로 추천 성공")
    void 새로운_경로_추천_성공() {
        // given
        String userCode = "TEST_USER_001";

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserCode).thenReturn(userCode);

            given(userRepository.findByUserCode(userCode))
                    .willReturn(Optional.of(testUser));

            given(routeSessionRepository.findByUserIdAndLogDate(testUser.getId(), today))
                    .willReturn(Optional.empty());

            given(routeLevelRepository.findByRouteLevel(testUser.getRecoveryLevel()))
                    .willReturn(Optional.of(testRouteLevel));

            given(kakaoApiClient.searchPlacesByCategory(
                    eq(requestDto.getLatitude()),
                    eq(requestDto.getLongitude()),
                    eq(testRouteLevel.getRecommendedMaxDistanceM()),
                    eq(10),
                    any(PlaceCategory.class)))
                    .willReturn(mockMapResponse);

            RouteSession savedSession = RouteSession.builder()
                    .id(1L)
                    .user(testUser)
                    .routeStatus(AssignmentStatus.ASSIGNED)
                    .routeLevel(testRouteLevel)
                    .logDate(today)
                    .content("테스트 카페을(를) 방문해보세요. (거리: 500m) 주소: 서울시 중구 세종대로 110")
                    .build();

            given(routeSessionRepository.save(any(RouteSession.class)))
                    .willReturn(savedSession);
            // when
            RouteRecommendResponseDto result = routeService.getRecommendation(requestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRouteId()).isEqualTo(1L);
            assertThat(result.getContent()).contains("테스트 카페을(를) 방문해보세요");
            assertThat(result.getOrigin()).isEqualTo("ROUTE");
            assertThat(result.getDate()).isEqualTo(today);
            assertThat(result.getStatus()).isEqualTo("ASSIGNED");

            // captor 확인
            ArgumentCaptor<RouteSession> sessionCaptor = ArgumentCaptor.forClass(RouteSession.class);
            verify(routeSessionRepository).save(sessionCaptor.capture());

            RouteSession capturedSession = sessionCaptor.getValue();
            assertThat(capturedSession.getUser()).isEqualTo(testUser);
            assertThat(capturedSession.getRouteStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
            assertThat(capturedSession.getLogDate()).isEqualTo(today);
        }
    }

    @Test
    @DisplayName("이미 기존의 경로 추천 성공")
    void 이미_기존의_경로_추천_성공() {
        // given
        String userCode = "TEST_USER_001";
        String existingContent = "기존 경로 내용";

        RouteSession existingSession = RouteSession.builder()
                .id(99L)
                .user(testUser)
                .routeStatus(AssignmentStatus.COMPLETED)
                .content(existingContent)
                .logDate(today)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserCode).thenReturn(userCode);

            given(userRepository.findByUserCode(userCode))
                    .willReturn(Optional.of(testUser));

            given(routeSessionRepository.findByUserIdAndLogDate(testUser.getId(), today))
                    .willReturn(Optional.of(existingSession));

            // when
            RouteRecommendResponseDto result = routeService.getRecommendation(requestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRouteId()).isEqualTo(99L);
            assertThat(result.getContent()).isEqualTo(existingContent);
            assertThat(result.getOrigin()).isEqualTo("ROUTE");
            assertThat(result.getStatus()).isEqualTo("COMPLETED");

            // Kakao API 호출되지 않았는지 검증
            verify(kakaoApiClient, never()).searchPlacesByCategory(
                    anyDouble(), anyDouble(), anyInt(), anyInt(), any());
            verify(routeSessionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Kakao API 검색 결과 없음 - 예외 발생")
    void getRecommendation_NoSearchResults_ThrowException() {
        // given
        String userCode = "TEST_USER_001";
        KakaoMapResponseDto emptyResponse = KakaoMapResponseDto.builder()
                .documents(Collections.emptyList())
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserCode).thenReturn(userCode);

            given(userRepository.findByUserCode(userCode))
                    .willReturn(Optional.of(testUser));

            given(routeSessionRepository.findByUserIdAndLogDate(testUser.getId(), today))
                    .willReturn(Optional.empty());

            given(routeLevelRepository.findByRouteLevel(testUser.getRecoveryLevel()))
                    .willReturn(Optional.of(testRouteLevel));

            given(kakaoApiClient.searchPlacesByCategory(
                    anyDouble(), anyDouble(), anyInt(), anyInt(), any()))
                    .willReturn(emptyResponse);

            // when
            RouteRecommendResponseDto result = routeService.getRecommendation(requestDto);

            // then
            assertThat(result).isNull();
            verify(routeSessionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Kakao API 응답이 null - 예외 발생")
    void getRecommendation_NullKakaoResponse_ThrowException() {
        // given
        String userCode = "TEST_USER_001";

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserCode).thenReturn(userCode);

            given(userRepository.findByUserCode(userCode))
                    .willReturn(Optional.of(testUser));

            given(routeSessionRepository.findByUserIdAndLogDate(testUser.getId(), today))
                    .willReturn(Optional.empty());

            given(routeLevelRepository.findByRouteLevel(testUser.getRecoveryLevel()))
                    .willReturn(Optional.of(testRouteLevel));

            given(kakaoApiClient.searchPlacesByCategory(
                    anyDouble(), anyDouble(), anyInt(), anyInt(), any()))
                    .willReturn(null);

            // when
            RouteRecommendResponseDto result = routeService.getRecommendation(requestDto);

            // then
            assertThat(result).isNull();
            verify(routeSessionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("경로 상세 조회 성공")
    void 경로_상세_조회_성공() {
        // given
        Long routeId = 1L;
        String userCode = "TEST_USER_001";
        String destinationGrid = "37.570000,127.000000";

        RouteSession routeSession = RouteSession.builder()
                .id(routeId)
                .user(testUser)
                .content("테스트 경로")
                .destinationGrid(destinationGrid)
                .logDate(today)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserCode).thenReturn(userCode);

            given(userRepository.findByUserCode(userCode))
                    .willReturn(Optional.of(testUser));

            given(routeSessionRepository.findById(routeId))
                    .willReturn(Optional.of(routeSession));

            // when
            RouteDetailResponseDto result = routeService.getDetailRoute(routeId, requestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRouteId()).isEqualTo(routeId);
            assertThat(result.getContent()).isEqualTo("테스트 경로");
            assertThat(result.getDate()).isEqualTo(today);
            assertThat(result.getLatitude()).isEqualTo(requestDto.getLatitude());
            assertThat(result.getLongitude()).isEqualTo(requestDto.getLongitude());
            assertThat(result.getEstimatedTime()).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("recoveryLevel이 1인 사용자에게는 추천하지 않음")
    void recoveryLevel이_1인_사용자에게는_추천하지_않음() {
        // given
        String userCode = "TEST_USER_002";
        User userWithLevel1 = User.builder()
                .id(2L)
                .userCode(userCode)
                .recoveryLevel(1)  // recoveryLevel 1
                .build();

        RouteLevel routeLevelForLevel1 = RouteLevel.builder()
                .id(1L)
                .routeLevel(1)
                .recommendedMaxDistanceM(0)  // distance가 0
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserCode).thenReturn(userCode);

            given(userRepository.findByUserCode(userCode))
                    .willReturn(Optional.of(userWithLevel1));

            given(routeSessionRepository.findByUserIdAndLogDate(userWithLevel1.getId(), today))
                    .willReturn(Optional.empty());

            given(routeLevelRepository.findByRouteLevel(1))
                    .willReturn(Optional.of(routeLevelForLevel1));

            // Kakao API가 빈 결과를 반환하도록 설정
            KakaoMapResponseDto emptyResponse = KakaoMapResponseDto.builder()
                    .documents(Collections.emptyList())
                    .build();

            given(kakaoApiClient.searchPlacesByCategory(
                    eq(requestDto.getLatitude()),
                    eq(requestDto.getLongitude()),
                    eq(0),  // recommendedMaxDistanceM이 0
                    eq(10),
                    any(PlaceCategory.class)))
                    .willReturn(emptyResponse);

            // when
            RouteRecommendResponseDto result = routeService.getRecommendation(requestDto);

            // then
            assertThat(result).isNull();

            // Kakao API가 distance 0으로 호출되었는지 검증
            verify(kakaoApiClient).searchPlacesByCategory(
                    eq(requestDto.getLatitude()),
                    eq(requestDto.getLongitude()),
                    eq(0),
                    eq(10),
                    any(PlaceCategory.class));

            // 저장이 일어나지 않았는지 검증
            verify(routeSessionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("목적지 도착 - 챌린지 완료")
    void end_Arrived_Success() {
        // given
        Long routeId = 1L;
        String challengeSessionId = "test-challenge-session-id";
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(30);
        String activeKey = "challenge:active:" + userCode;
        String detailKey = "challenge:session:" + challengeSessionId;

        // 목적지 좌표 (거의 같은 위치 - 30m 이내)
        double destinationY = 37.5666; // 약 11m 차이
        double destinationX = 126.9781;

        RouteLocationRequestDto locationRequest = RouteLocationRequestDto.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("userCode", userCode);
        sessionData.put("routeId", routeId.toString());
        sessionData.put("startedAt", startedAt.toString());
        sessionData.put("destinationY", String.valueOf(destinationY));
        sessionData.put("destinationX", String.valueOf(destinationX));

        RouteSession routeSession = RouteSession.builder()
                .id(routeId)
                .user(testUser)
                .routeStatus(AssignmentStatus.PROGRESS)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserCode).thenReturn(userCode);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForHash()).willReturn(hashOperations);

            given(valueOperations.get(activeKey))
                    .willReturn(challengeSessionId);

            given(hashOperations.entries(detailKey))
                    .willReturn(sessionData);

            given(routeSessionRepository.findById(routeId))
                    .willReturn(Optional.of(routeSession));

            // when
            RouteChallengeResponseDto result = routeService.end(routeId, locationRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getChallengeSessionId()).isEqualTo(challengeSessionId);
            assertThat(result.getRouteSessionId()).isEqualTo(routeId);
            assertThat(result.getAssignmentStatus()).isEqualTo("COMPLETED");
            assertThat(result.getStartedAt()).isEqualTo(startedAt);

            // Redis 삭제 검증
            verify(redisTemplate).delete(activeKey);
            verify(redisTemplate).delete(detailKey);
            // 이벤트 발행 검증
            ArgumentCaptor<ChallengeCompletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(ChallengeCompletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
        }
    }

    @Test
    @DisplayName("목적지 미도착 - 챌린지 취소")
    void end_NotArrived_Canceled() {
        // given
        Long routeId = 1L;
        String challengeSessionId = "test-challenge-session-id";
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(30);
        String activeKey = "challenge:active:" + userCode;
        String detailKey = "challenge:session:" + challengeSessionId;

        // 목적지 좌표 (300m 이상)
        double destinationY = 37.5695; // 약 333m 차이
        double destinationX = 126.9780;

        RouteLocationRequestDto locationRequest = RouteLocationRequestDto.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("userCode", userCode);
        sessionData.put("routeId", routeId.toString());
        sessionData.put("startedAt", startedAt.toString());
        sessionData.put("destinationY", String.valueOf(destinationY));
        sessionData.put("destinationX", String.valueOf(destinationX));

        RouteSession routeSession = RouteSession.builder()
                .id(routeId)
                .user(testUser)
                .routeStatus(AssignmentStatus.PROGRESS)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserCode).thenReturn(userCode);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForHash()).willReturn(hashOperations);

            given(valueOperations.get(activeKey)).willReturn(challengeSessionId);

            given(hashOperations.entries(detailKey))
                    .willReturn(sessionData);

            given(routeSessionRepository.findById(routeId))
                    .willReturn(Optional.of(routeSession));

            // when
            RouteChallengeResponseDto result = routeService.end(routeId, locationRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getChallengeSessionId()).isEqualTo(challengeSessionId);
            assertThat(result.getRouteSessionId()).isEqualTo(routeId);
            assertThat(result.getAssignmentStatus()).isEqualTo("CANCELED");
            assertThat(result.getStartedAt()).isEqualTo(startedAt);

            // Redis 삭제 검증
            verify(redisTemplate).delete(activeKey);
            verify(redisTemplate).delete(detailKey);
        }
    }

    @Test
    @DisplayName("경계값 테스트 - 정확히 100m 거리 시 취소")
    void end_ExactlyThreshold_Completed() {
        // given
        Long routeId = 1L;
        String challengeSessionId = "test-challenge-session-id";
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(30);
        String activeKey = "challenge:active:" + userCode;
        String detailKey = "challenge:session:" + challengeSessionId;

        // 정확히 100m 떨어진 좌표 (위도 기준 약 0.0009도)
        double destinationY = 37.5674;
        double destinationX = 126.9780;

        RouteLocationRequestDto locationRequest = RouteLocationRequestDto.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("userCode", userCode);
        sessionData.put("routeId", routeId.toString());
        sessionData.put("startedAt", startedAt.toString());
        sessionData.put("destinationY", String.valueOf(destinationY));
        sessionData.put("destinationX", String.valueOf(destinationX));

        RouteSession routeSession = RouteSession.builder()
                .id(routeId)
                .user(testUser)
                .routeStatus(AssignmentStatus.PROGRESS)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserCode).thenReturn(userCode);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForHash()).willReturn(hashOperations);

            given(valueOperations.get(activeKey))
                    .willReturn(challengeSessionId);

            given(hashOperations.entries(detailKey))
                    .willReturn(sessionData);

            given(routeSessionRepository.findById(routeId))
                    .willReturn(Optional.of(routeSession));

            // when
            RouteChallengeResponseDto result = routeService.end(routeId, locationRequest);

            // then
            assertThat(result.getAssignmentStatus()).isEqualTo("CANCELED");
        }
    }
}
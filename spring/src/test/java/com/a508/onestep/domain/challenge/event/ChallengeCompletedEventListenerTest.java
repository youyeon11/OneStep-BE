package com.a508.onestep.domain.challenge.event;

import com.a508.onestep.domain.challenge.dto.request.ChallengeCompleteRequestDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeCompleteResponseDto;
import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import com.a508.onestep.domain.challenge.repository.ChallengeAssignmentRepository;
import com.a508.onestep.domain.challenge.service.ChallengeServiceImpl;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContext;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeCompletedEventListenerTest {

    @Mock private ChallengeAssignmentRepository challengeAssignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks ChallengeServiceImpl challengeService;

    private User testUser;
    private static final String TEST_USER_CODE = "USER123";
    private static final String TEST_ROLE = "ROLE_USER";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode(TEST_USER_CODE)
                .totalExp(100)
                .build();

        // UserContext 설정
        UserContext userContext = UserContext.builder()
                .userCode(TEST_USER_CODE)
                .role(TEST_ROLE)
                .build();
        UserContextHolder.set(userContext);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Nested
    @DisplayName("챌린지 완료 테스트")
    class CompleteTest {

        @Test
        @DisplayName("성공: 챌린지를 완료하고 이벤트를 발행한다")
        void complete_Success() {
            // given
            ChallengeCompleteRequestDto requestDto = ChallengeCompleteRequestDto.builder()
                    .challengeId(1L)
                    .emotion(5)
                    .build();

            ChallengeAssignment challenge = ChallengeAssignment.builder()
                    .id(1L)
                    .user(testUser)
                    .content("운동하기")
                    .challengeStatus(AssignmentStatus.ASSIGNED)
                    .exp(50)
                    .build();

            when(userRepository.findByUserCode(TEST_USER_CODE))
                    .thenReturn(Optional.of(testUser));
            when(challengeAssignmentRepository.findById(1L))
                    .thenReturn(Optional.of(challenge));

            // when
            ChallengeCompleteResponseDto result = challengeService.complete(requestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getChallengeStatus()).isEqualTo("COMPLETED");

            // 이벤트 발행 검증
            ArgumentCaptor<ChallengeCompletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(ChallengeCompletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            ChallengeCompletedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getUser().getUserCode()).isEqualTo(testUser.getUserCode());
            assertThat(publishedEvent.getChallengeId()).isEqualTo(1L);
            assertThat(publishedEvent.getEarnedExp()).isEqualTo(50);
            assertThat(publishedEvent.getEmotion()).isEqualTo(5);
            assertThat(publishedEvent.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패: UserContext가 없으면 예외 발생")
        void complete_NoUserContext_ThrowsException() {
            // given
            UserContextHolder.clear();

            ChallengeCompleteRequestDto requestDto = ChallengeCompleteRequestDto.builder()
                    .challengeId(1L)
                    .emotion(5)
                    .build();

            // when & then
            assertThatThrownBy(() -> challengeService.complete(requestDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.UNAUTHORIZED);

            verify(userRepository, never()).findByUserCode(anyString());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("실패: 챌린지를 찾을 수 없으면 예외 발생")
        void complete_ChallengeNotFound_ThrowsException() {
            // given
            ChallengeCompleteRequestDto requestDto = ChallengeCompleteRequestDto.builder()
                    .challengeId(999L)
                    .emotion(5)
                    .build();

            when(userRepository.findByUserCode(TEST_USER_CODE))
                    .thenReturn(Optional.of(testUser));
            when(challengeAssignmentRepository.findById(999L))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> challengeService.complete(requestDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.CHALLENGE_NOT_FOUND);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("실패: 다른 사용자의 챌린지를 완료하려고 하면 예외 발생")
        void complete_Unauthorized_ThrowsException() {
            // given
            User otherUser = User.builder()
                    .id(2L)
                    .userCode("OTHER_USER")
                    .totalExp(50)
                    .build();

            ChallengeCompleteRequestDto requestDto = ChallengeCompleteRequestDto.builder()
                    .challengeId(1L)
                    .emotion(5)
                    .build();

            ChallengeAssignment otherUserChallenge = ChallengeAssignment.builder()
                    .id(1L)
                    .user(otherUser) // 다른 사용자의 챌린지
                    .content("운동하기")
                    .challengeStatus(AssignmentStatus.ASSIGNED)
                    .build();

            when(userRepository.findByUserCode(TEST_USER_CODE))
                    .thenReturn(Optional.of(testUser));
            when(challengeAssignmentRepository.findById(1L))
                    .thenReturn(Optional.of(otherUserChallenge));

            // when & then
            assertThatThrownBy(() -> challengeService.complete(requestDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.UNAUTHORIZED);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("성공: 완료된 챌린지의 상태가 COMPLETED로 변경된다")
        void complete_StatusChangedToCompleted() {
            // given
            ChallengeCompleteRequestDto requestDto = ChallengeCompleteRequestDto.builder()
                    .challengeId(1L)
                    .emotion(5)
                    .build();

            ChallengeAssignment challenge = createChallengeAssignment(
                    1L, "운동하기", AssignmentStatus.ASSIGNED);

            when(userRepository.findByUserCode(TEST_USER_CODE))
                    .thenReturn(Optional.of(testUser));
            when(challengeAssignmentRepository.findById(1L))
                    .thenReturn(Optional.of(challenge));

            // when
            ChallengeCompleteResponseDto result = challengeService.complete(requestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getChallengeStatus()).isEqualTo("COMPLETED");

            // complete() 메서드가 호출되었는지는 이벤트 발행으로 간접 검증
            verify(eventPublisher).publishEvent(any(ChallengeCompletedEvent.class));
        }

        @Test
        @DisplayName("성공: 여러 감정 타입으로 챌린지를 완료할 수 있다")
        void complete_WithDifferentEmotions() {
            // given
            Integer[] emotions = {5, 4, 3};

            for (int i = 0; i < emotions.length; i++) {
                Long challengeId = (long) (i + 1);
                ChallengeCompleteRequestDto requestDto = ChallengeCompleteRequestDto.builder()
                        .challengeId(challengeId)
                        .emotion(emotions[i])
                        .build();
                ChallengeAssignment challenge = createChallengeAssignment(
                        challengeId, "챌린지 " + (i + 1), AssignmentStatus.ASSIGNED);

                when(userRepository.findByUserCode(TEST_USER_CODE))
                        .thenReturn(Optional.of(testUser));
                when(challengeAssignmentRepository.findById(challengeId))
                        .thenReturn(Optional.of(challenge));

                // when
                ChallengeCompleteResponseDto result = challengeService.complete(requestDto);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getChallengeStatus()).isEqualTo("COMPLETED");

                ArgumentCaptor<ChallengeCompletedEvent> eventCaptor =
                        ArgumentCaptor.forClass(ChallengeCompletedEvent.class);
                verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());

                ChallengeCompletedEvent event = eventCaptor.getValue();
                assertThat(event.getEmotion()).isEqualTo(emotions[i]);
            }
        }
    }

    @Nested
    @DisplayName("UserContext 테스트")
    class UserContextTest {

        @Test
        @DisplayName("성공: UserContext가 올바르게 설정되고 조회된다")
        void userContext_IsCorrectlySetAndRetrieved() {
            // given & when
            String userCode = UserContextHolder.getUserCode();
            String role = UserContextHolder.getRole();
            boolean isAuthenticated = UserContextHolder.isAuthenticated();

            // then
            assertThat(userCode).isEqualTo(TEST_USER_CODE);
            assertThat(role).isEqualTo(TEST_ROLE);
            assertThat(isAuthenticated).isTrue();
        }

        @Test
        @DisplayName("성공: UserContext를 clear하면 인증되지 않은 상태가 된다")
        void userContext_ClearMakesUnauthenticated() {
            // given
            assertThat(UserContextHolder.isAuthenticated()).isTrue();

            // when
            UserContextHolder.clear();

            // then
            assertThat(UserContextHolder.isAuthenticated()).isFalse();
            assertThatThrownBy(() -> UserContextHolder.getUserCode())
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패: UserContext가 없을 때 getUserCode() 호출하면 예외 발생")
        void userContext_GetUserCodeWithoutContext_ThrowsException() {
            // given
            UserContextHolder.clear();

            // when & then
            assertThatThrownBy(() -> UserContextHolder.getUserCode())
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패: UserContext가 없을 때 getRole() 호출하면 예외 발생")
        void userContext_GetRoleWithoutContext_ThrowsException() {
            // given
            UserContextHolder.clear();

            // when & then
            assertThatThrownBy(() -> UserContextHolder.getRole())
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }
    }

    // Helper method
    private ChallengeAssignment createChallengeAssignment(
            Long id, String content, AssignmentStatus status) {
        return spy(ChallengeAssignment.builder()
                .id(id)
                .user(testUser)
                .content(content)
                .origin(Origin.SELF)
                .challengeStatus(status)
                .exp(50)
                .assignedDate(LocalDate.now())
                .build());
    }
}
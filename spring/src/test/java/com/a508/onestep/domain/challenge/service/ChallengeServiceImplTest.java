package com.a508.onestep.domain.challenge.service;

import com.a508.onestep.domain.challenge.dto.request.ChallengeCompleteRequestDto;
import com.a508.onestep.domain.challenge.dto.request.ChallengeRequestDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeCompleteResponseDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeResponseDto;
import com.a508.onestep.domain.challenge.dto.response.InitialChallengeResponseDto;
import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import com.a508.onestep.domain.challenge.entity.ChallengeMaster;
import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.challenge.repository.ChallengeAssignmentRepository;
import com.a508.onestep.domain.challenge.repository.ChallengeMasterRepository;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.common.TagCategory;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContext;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

    @Mock private ChallengeAssignmentRepository challengeAssignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ChallengeMasterRepository challengeMasterRepository;
    @InjectMocks private ChallengeServiceImpl challengeService;

    private User testUser;
    private static final String TEST_USER_CODE = "USER123";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode(TEST_USER_CODE)
                .recoveryLevel(2)
                .totalExp(100)
                .build();

        // UserContext 설정
        UserContext userContext = UserContext.builder()
                .userCode(TEST_USER_CODE)
                .build();
        UserContextHolder.set(userContext);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("챌린지 등록 성공")
    void 챌린지_등록_성공() {
        // given
        ChallengeRequestDto requestDto = ChallengeRequestDto.builder()
                .content("운동하기")
                .build();

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));

        when(challengeAssignmentRepository.save(any(ChallengeAssignment.class)))
                .thenAnswer(invocation -> {
                    ChallengeAssignment challenge = invocation.getArgument(0);
                    return ChallengeAssignment.builder()
                            .id(1L)
                            .user(challenge.getUser())
                            .content(challenge.getContent())
                            .origin(challenge.getOrigin())
                            .challengeStatus(challenge.getChallengeStatus())
                            .exp(challenge.getExp())
                            .assignedDate(challenge.getAssignedDate())
                            .build();
                });

        // when
        ChallengeResponseDto result = challengeService.register(requestDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getChallengeId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("운동하기");
        assertThat(result.getOrigin()).isEqualTo("SELF");
        assertThat(result.getChallengeStatus()).isEqualTo("ASSIGNED");
        assertThat(result.getExp()).isEqualTo(0);

        verify(userRepository).findByUserCode(TEST_USER_CODE);
        verify(challengeAssignmentRepository).save(any(ChallengeAssignment.class));
    }

    @Test
    @DisplayName("존재 하지 않는 사용자 - 실패")
    void 존재하지_않는_사용자_실패() {
        // given
        ChallengeRequestDto requestDto = ChallengeRequestDto.builder()
                .content("운동하기")
                .build();

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> challengeService.register(requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting("baseCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findByUserCode(TEST_USER_CODE);
        verify(challengeAssignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("등록 시 오늘날짜 기록 성공")
    void 등록시_오늘날짜_기록_성공() {
        // given
        ChallengeRequestDto requestDto = ChallengeRequestDto.builder()
                .content("독서하기")
                .build();

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));

        ArgumentCaptor<ChallengeAssignment> captor = ArgumentCaptor.forClass(ChallengeAssignment.class);

        ChallengeAssignment savedChallenge = ChallengeAssignment.builder()
                .id(1L)
                .user(testUser)
                .content("독서하기")
                .origin(Origin.SELF)
                .challengeStatus(AssignmentStatus.ASSIGNED)
                .exp(0)
                .assignedDate(LocalDate.now())
                .build();

        when(challengeAssignmentRepository.save(any(ChallengeAssignment.class)))
                .thenReturn(savedChallenge);

        // when
        challengeService.register(requestDto);

        // then
        verify(challengeAssignmentRepository).save(captor.capture());
        ChallengeAssignment captured = captor.getValue();
        assertThat(captured.getAssignedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("오늘 날짜에 내가 등록한 챌린지 조회")
    void 오늘_날짜_내가등록한_챌린지_조회_성공() {
        // given
        LocalDate today = LocalDate.now();

        List<ChallengeAssignment> challengeList = List.of(
                createChallengeAssignment(1L, "운동하기", AssignmentStatus.ASSIGNED),
                createChallengeAssignment(2L, "독서하기", AssignmentStatus.COMPLETED),
                createChallengeAssignment(3L, "명상하기", AssignmentStatus.ASSIGNED)
        );

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));
        when(challengeAssignmentRepository.findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF))
                .thenReturn(challengeList);

        // when
        List<ChallengeResponseDto> result = challengeService.getAll();

        // then
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting("content")
                .containsExactly("운동하기", "독서하기", "명상하기");

        verify(userRepository).findByUserCode(TEST_USER_CODE);
        verify(challengeAssignmentRepository).findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF);
    }

    @Test
    @DisplayName("챌린지가 없으면 빈 리스트를 반환한다")
    void 챌린지가_없으면_빈리스트를_반환() {
        // given
        LocalDate today = LocalDate.now();

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));
        when(challengeAssignmentRepository.findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF))
                .thenReturn(Collections.emptyList());

        // when
        List<ChallengeResponseDto> result = challengeService.getAll();

        // then
        assertThat(result).isEmpty();
        verify(challengeAssignmentRepository).findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF);
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 예외 발생")
    void 사용자를_찾을수없으면_예외() {
        // given
        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> challengeService.getAll())
                .isInstanceOf(BusinessException.class)
                .extracting("baseCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(challengeAssignmentRepository, never())
                .findSelfByUserIdAndAssignedDate(anyLong(), any(LocalDate.class), any(Origin.class));
    }

    @Test
    @DisplayName("챌린지를 완료하고 이벤트를 발행한다")
    void 챌린지_완료_후_이벤트_발행() {
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
        assertThat(publishedEvent.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(publishedEvent.getChallengeId()).isEqualTo(1L);
        assertThat(publishedEvent.getEarnedExp()).isEqualTo(50);
        assertThat(publishedEvent.getEmotion()).isEqualTo(5);
    }

    @Test
    @DisplayName("챌린지를 찾을 수 없으면 예외 발생")
    void 챌린지를_찾을수없으면_예외() {
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
    @DisplayName("다른 사용자의 챌린지를 완료하려고 하면 예외 발생")
    void 다른사용자의_챌린지를_완료하려고하면_예외() {
        // given
        User otherUser = User.builder()
                .id(2L)
                .userCode("OTHER_USER")
                .totalExp(50)
                .build();

        ChallengeCompleteRequestDto requestDto = ChallengeCompleteRequestDto.builder()
                .challengeId(1L)
                .emotion(1)
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
    @DisplayName("챌린지 완료 후 상태 확인")
    void 챌린지_완료_후_상태_확인() {
        // given
        ChallengeCompleteRequestDto requestDto = ChallengeCompleteRequestDto.builder()
                .challengeId(1L)
                .emotion(1)
                .build();

        ChallengeAssignment challenge = spy(createChallengeAssignment(
                1L, "운동하기", AssignmentStatus.ASSIGNED));

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));
        when(challengeAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(challenge));

        // when
        challengeService.complete(requestDto);

        // then
        verify(challenge).complete(1);
    }


    private ChallengeAssignment createChallengeAssignment(
            Long id, String content, AssignmentStatus status) {
        return ChallengeAssignment.builder()
                .id(id)
                .user(testUser)
                .content(content)
                .origin(Origin.SELF)
                .challengeStatus(status)
                .exp(50)
                .assignedDate(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("이미 완료된 챌린지를 다시 완료하려고 하면 예외 발생")
    void 이미_완료된_챌린지를_다시완료시_예외() {
        // given
        ChallengeCompleteRequestDto requestDto = ChallengeCompleteRequestDto.builder()
                .challengeId(1L)
                .emotion(5)
                .build();

        // 이미 완료된 챌린지 생성
        ChallengeAssignment completedChallenge = ChallengeAssignment.builder()
                .id(1L)
                .user(testUser)
                .content("운동하기")
                .challengeStatus(AssignmentStatus.COMPLETED) // 이미 완료됨
                .origin(Origin.SELF)
                .exp(50)
                .assignedDate(LocalDate.now())
                .build();

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));
        when(challengeAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(completedChallenge));

        // when
        BusinessException exception = catchThrowableOfType(
                () -> challengeService.complete(requestDto),
                BusinessException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getBaseCode()).isEqualTo(ErrorCode.CHALLENGE_ALREADY_DONE);
        assertThat(exception.getMessage()).contains("이미 해당 챌린지를 완료했습니다.");

        // User 경험치가 업데이트되지 않았는지 확인
        assertThat(testUser.getTotalExp()).isEqualTo(100); // 초기값 그대로

        // 이벤트가 발행되지 않았는지 확인
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("성공: Origin이 SELF가 아닌 챌린지는 조회되지 않는다")
    void Origin이_SELF가아닌_챌린지는_조회되지않는다() {
        // given
        LocalDate today = LocalDate.now();
        List<ChallengeAssignment> selfOnlyChallenges = List.of(
                ChallengeAssignment.builder()
                        .id(1L)
                        .user(testUser)
                        .content("SELF 챌린지")
                        .origin(Origin.SELF)
                        .challengeStatus(AssignmentStatus.ASSIGNED)
                        .exp(50)
                        .assignedDate(LocalDate.now())
                        .build()
        );

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));
        when(challengeAssignmentRepository.findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF))
                .thenReturn(selfOnlyChallenges);

        // when
        List<ChallengeResponseDto> result = challengeService.getAll();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrigin()).isEqualTo("SELF");

        // Repository 메서드가 정확히 호출되었는지만 확인
        verify(challengeAssignmentRepository).findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF);
    }

    @Test
    @DisplayName("올바른 Repository 메서드를 호출 성공")
    void getAll_올바른_Repository_메서드_호출_성공() {
        // given
        LocalDate today = LocalDate.now();

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));
        when(challengeAssignmentRepository.findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF))
                .thenReturn(Collections.emptyList());

        // when
        challengeService.getAll();

        // then
        verify(challengeAssignmentRepository).findSelfByUserIdAndAssignedDate(
                eq(testUser.getId()),
                eq(today),
                eq(Origin.SELF)
        );

        // 다른 메서드는 호출되지 않았는지 확인
        verify(challengeAssignmentRepository, only())
                .findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF);
    }

    @Test
    @DisplayName("SELF Origin 챌린지가 없으면 빈 리스트를 반환한다")
    void getAll_EmptyList_WhenNoSelfChallenges() {
        // given
        LocalDate today = LocalDate.now();

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));
        when(challengeAssignmentRepository.findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF))
                .thenReturn(Collections.emptyList());

        // when
        List<ChallengeResponseDto> result = challengeService.getAll();

        // then
        assertThat(result).isEmpty();
        verify(challengeAssignmentRepository).findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF);
    }

    @Test
    @DisplayName("여러 상태의 SELF Origin 챌린지를 모두 조회한다")
    void getAll_MultipleStatusesWithSelfOrigin() {
        // given
        LocalDate today = LocalDate.now();

        ChallengeAssignment challengeAssignment1 = ChallengeAssignment.builder()
                .id(1L)
                .user(testUser)
                .content("완료된 SELF")
                .origin(Origin.SELF)
                .challengeStatus(AssignmentStatus.COMPLETED)
                .exp(50)
                .assignedDate(LocalDate.now())
                .build();

        ChallengeAssignment challengeAssignment2 = ChallengeAssignment.builder()
                .id(2L)
                .user(testUser)
                .content("진행중 SELF")
                .origin(Origin.SELF)
                .challengeStatus(AssignmentStatus.ASSIGNED)
                .exp(50)
                .assignedDate(LocalDate.now())
                .build();

        ChallengeAssignment challengeAssignment3 = ChallengeAssignment.builder()
                .id(3L)
                .user(testUser)
                .content("또 다른 SELF")
                .origin(Origin.SELF)
                .challengeStatus(AssignmentStatus.ASSIGNED)
                .exp(50)
                .assignedDate(LocalDate.now())
                .build();

        List<ChallengeAssignment> challengeList = List.of(
                challengeAssignment1,
                challengeAssignment2,
                challengeAssignment3
        );

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));
        when(challengeAssignmentRepository.findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF))
                .thenReturn(challengeList);

        // when
        List<ChallengeResponseDto> result = challengeService.getAll();

        // then
        assertThat(result).hasSize(3);  // 3개 모두 SELF

        assertThat(result)
                .allMatch(dto -> dto.getOrigin().equals("SELF"));
        assertThat(result)
                .extracting("challengeStatus")
                .containsExactly("COMPLETED", "ASSIGNED", "ASSIGNED");

        assertThat(result)
                .extracting("content")
                .containsExactly("완료된 SELF", "진행중 SELF", "또 다른 SELF");

        verify(challengeAssignmentRepository).findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF);
    }

    @Test
    @DisplayName("Repository가 SELF만 반환하고 다른 Origin은 제외한다")
    void getAll_OnlySelfOrigin_ExcludesOthers() {
        // given
        LocalDate today = LocalDate.now();

        ChallengeAssignment selfChallenge1 = ChallengeAssignment.builder()
                .id(1L)
                .user(testUser)
                .content("SELF 챌린지 1")
                .origin(Origin.SELF)
                .challengeStatus(AssignmentStatus.COMPLETED)
                .exp(50)
                .assignedDate(LocalDate.now())
                .build();

        ChallengeAssignment selfChallenge2 = ChallengeAssignment.builder()
                .id(2L)
                .user(testUser)
                .content("SELF 챌린지 2")
                .origin(Origin.SELF)
                .challengeStatus(AssignmentStatus.ASSIGNED)
                .exp(50)
                .assignedDate(LocalDate.now())
                .build();

        List<ChallengeAssignment> selfOnlyList = List.of(selfChallenge1, selfChallenge2);

        when(userRepository.findByUserCode(TEST_USER_CODE))
                .thenReturn(Optional.of(testUser));
        when(challengeAssignmentRepository.findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF))
                .thenReturn(selfOnlyList);

        // when
        List<ChallengeResponseDto> result = challengeService.getAll();

        // then
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting("origin")
                .containsOnly("SELF")
                .doesNotContain("RECOMMENDED", "ROUTE");

        // 모든 챌린지가 SELF인지 개별 확인
        assertThat(result)
                .allMatch(dto -> dto.getOrigin().equals("SELF"));

        verify(challengeAssignmentRepository).findSelfByUserIdAndAssignedDate(testUser.getId(), today, Origin.SELF);
    }

    @Test
    @DisplayName("초기 유저에게 20개 반환 성공")
    void 초기유저에게보낼_20개_반환_성공() {
        // given
        List<ChallengeMaster> challengeMasters = IntStream.range(0, 30)
                .mapToObj(i -> ChallengeMaster.builder()
                        .id((long) i)
                        .title("challenge-" + i)
                        .difficultyLevel(2)
                        .build())
                .toList();

        given(userRepository.findByUserCode("USER123"))
                .willReturn(Optional.of(testUser));

        List<TagCategory> categories = List.of(TagCategory.values());
        given(challengeMasterRepository.findRecommendedChallenges(2, categories, testUser))
                .willReturn(new ArrayList<>(challengeMasters));

        // when
        List<InitialChallengeResponseDto> result =
                challengeService.getInitialRecommendations();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(20);

        assertThat(result)
                .extracting(InitialChallengeResponseDto::getContent)
                .allMatch(content -> content.startsWith("challenge-"));

        verify(userRepository).findByUserCode("USER123");
        verify(challengeMasterRepository).findRecommendedChallenges(2, categories, testUser);
    }
}
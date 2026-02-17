package com.a508.onestep.domain.user.service;

import com.a508.onestep.domain.user.dto.request.UserResurveyRequestDto;
import com.a508.onestep.domain.user.dto.request.UserSurveyRequestDto;
import com.a508.onestep.domain.user.dto.response.UserInfoResponseDto;
import com.a508.onestep.domain.user.entity.SurveyLog;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.SurveyRepository;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("설문조사 서비스 테스트")
class UserServiceImplTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private MockedStatic<UserContextHolder> mockedUserContext;

    private User testUser;
    private static final String TEST_USER_CODE = "TEST_USER_001";
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Mock UserContext
        mockedUserContext = mockStatic(UserContextHolder.class);
        mockedUserContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

        testUser = User.builder()
                .id(TEST_USER_ID)
                .userCode(TEST_USER_CODE)
                .nickname("테스트유저")
                .gpsOptIn(true)
                .notifOptIn(true)
                .build();
        ReflectionTestUtils.setField(testUser, "createdAt", LocalDateTime.now().minusDays(30));
    }

    @AfterEach
    void tearDown() {
        mockedUserContext.close();
    }

    @Test
    @DisplayName("초기 설문조사 등록 성공 - 총점 20점 이하 (회복레벨 1)")
    void registerSurvey_Success_RecoveryLevel1() {
        // given
        List<Integer> answers = Arrays.asList(1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1); // 총점 11
        UserSurveyRequestDto requestDto = UserSurveyRequestDto.builder()
                .answers(answers)
                .build();

        given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(testUser));
        given(surveyRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.registerSurvey(requestDto);

        // then
        then(userRepository).should(times(1)).findByUserCode(TEST_USER_CODE);

        ArgumentCaptor<List<SurveyLog>> captor = ArgumentCaptor.forClass(List.class);
        then(surveyRepository).should(times(1)).saveAll(captor.capture());

        List<SurveyLog> savedLogs = captor.getValue();
        assertThat(savedLogs).hasSize(15);
        assertThat(savedLogs.get(0).getSurveyNumber()).isEqualTo(1);
        assertThat(savedLogs.get(14).getSurveyNumber()).isEqualTo(15);
        assertThat(testUser.getRecoveryLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("초기 설문조사 등록 성공 - 총점 21~30점 (회복레벨 2)")
    void registerSurvey_Success_RecoveryLevel2() {
        // given
        List<Integer> answers = Arrays.asList(2, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 1, 1); // 총점 22
        UserSurveyRequestDto requestDto = UserSurveyRequestDto.builder()
                .answers(answers)
                .build();

        given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(testUser));
        given(surveyRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.registerSurvey(requestDto);

        // then
        assertThat(testUser.getRecoveryLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("초기 설문조사 등록 성공 - 총점 31점 이상 (회복레벨 3)")
    void registerSurvey_Success_RecoveryLevel3() {
        // given
        List<Integer> answers = Arrays.asList(3, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 2, 2); // 총점 37
        UserSurveyRequestDto requestDto = UserSurveyRequestDto.builder()
                .answers(answers)
                .build();

        given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(testUser));
        given(surveyRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.registerSurvey(requestDto);

        // then
        assertThat(testUser.getRecoveryLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("재설문 성공 - 답변 업데이트 및 회복레벨 재산정")
    void resurvey_Success() {
        // given
        Integer surveyNumber = 5;
        Integer newAnswer = 3;

        UserResurveyRequestDto requestDto = UserResurveyRequestDto.builder()
                .surveyNumber(surveyNumber)
                .answer(newAnswer)
                .build();

        // 실제 SurveyLog 객체 생성 (Mock 대신)
        // 5번 문항: 1점, 나머지: 2점 -> 총점 29점 (회복레벨 2)
        List<SurveyLog> existingLogs = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> SurveyLog.builder()
                        .id((long) i)
                        .user(testUser)
                        .surveyNumber(i)
                        .answer(i == surveyNumber ? 1 : 2)
                        .build())
                .collect(Collectors.toList());

        SurveyLog targetLog = existingLogs.get(surveyNumber - 1);

        given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(testUser));
        given(surveyRepository.findBySurveyNumberAndUser(surveyNumber, TEST_USER_ID))
                .willReturn(Optional.of(targetLog));
        given(surveyRepository.findByUserId(TEST_USER_ID)).willReturn(existingLogs);

        // when
        UserInfoResponseDto result = userService.resurvey(requestDto);

        // then
        then(userRepository).should(times(1)).findByUserCode(TEST_USER_CODE);
        then(surveyRepository).should(times(1)).findBySurveyNumberAndUser(surveyNumber, TEST_USER_ID);
        then(surveyRepository).should(times(1)).findByUserId(TEST_USER_ID);

        // 5번 문항: 1점 -> 3점 변경 (+2점)
        // 총점: 29점 -> 31점 -> 회복레벨 3
        assertThat(targetLog.getAnswer()).isEqualTo(newAnswer);
        assertThat(testUser.getRecoveryLevel()).isEqualTo(3);

        assertThat(result).isNotNull();
        assertThat(result.getNickname()).isEqualTo(testUser.getNickname());
        assertThat(result.getIsLocationAllowed()).isTrue();
        assertThat(result.getIsAlarmAllowed()).isTrue();
        assertThat(result.getDaysTogether()).isEqualTo(30);
    }

    @Test
    @DisplayName("재설문 성공 - 회복레벨 변경 확인 (2 -> 3)")
    void resurvey_Success_RecoveryLevelChange() {
        // given
        testUser.updateSurveyResult(25); // 초기 회복레벨 2

        Integer surveyNumber = 10;
        Integer oldAnswer = 1;
        Integer newAnswer = 3;

        UserResurveyRequestDto requestDto = UserResurveyRequestDto.builder()
                .surveyNumber(surveyNumber)
                .answer(newAnswer)
                .build();

        // 실제 SurveyLog 객체 생성
        // 10번 문항: 1점, 나머지: 2점 -> 총점 29점 (회복레벨 2)
        List<SurveyLog> existingLogs = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> SurveyLog.builder()
                        .id((long) i)
                        .user(testUser)
                        .surveyNumber(i)
                        .answer(i == surveyNumber ? oldAnswer : 2)
                        .build())
                .collect(Collectors.toList());

        SurveyLog targetLog = existingLogs.get(surveyNumber - 1);

        given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(testUser));
        given(surveyRepository.findBySurveyNumberAndUser(surveyNumber, TEST_USER_ID))
                .willReturn(Optional.of(targetLog));
        given(surveyRepository.findByUserId(TEST_USER_ID)).willReturn(existingLogs);

        // when
        UserInfoResponseDto result = userService.resurvey(requestDto);

        // then
        // 10번 문항: 1점 -> 3점 변경 (+2점)
        // 총점: 29점 -> 31점 -> 회복레벨 3
        assertThat(targetLog.getAnswer()).isEqualTo(newAnswer);
        assertThat(testUser.getRecoveryLevel()).isEqualTo(3);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("재설문 성공 - 총점 재계산 검증")
    void resurvey_Success_TotalScoreRecalculation() {
        // given
        Integer surveyNumber = 3;
        Integer oldAnswer = 2;
        Integer newAnswer = 0;

        UserResurveyRequestDto requestDto = UserResurveyRequestDto.builder()
                .surveyNumber(surveyNumber)
                .answer(newAnswer)
                .build();

        // 실제 SurveyLog 객체 생성
        // 모든 문항 2점 -> 총점 30점 (회복레벨 2)
        List<SurveyLog> existingLogs = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> SurveyLog.builder()
                        .id((long) i)
                        .user(testUser)
                        .surveyNumber(i)
                        .answer(oldAnswer)
                        .build())
                .collect(Collectors.toList());

        SurveyLog targetLog = existingLogs.get(surveyNumber - 1);

        given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(testUser));
        given(surveyRepository.findBySurveyNumberAndUser(surveyNumber, TEST_USER_ID))
                .willReturn(Optional.of(targetLog));
        given(surveyRepository.findByUserId(TEST_USER_ID)).willReturn(existingLogs);

        // when
        userService.resurvey(requestDto);

        // then
        // 3번 문항: 2점 -> 0점 변경 (-2점)
        // 총점: 30점 -> 28점 -> 회복레벨 2
        assertThat(targetLog.getAnswer()).isEqualTo(newAnswer);
        assertThat(testUser.getRecoveryLevel()).isEqualTo(2);
    }
}
package com.a508.onestep.domain.signal.listener;

import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.signal.entity.UserSignalLog;
import com.a508.onestep.domain.signal.event.UserSignalLogEvent;
import com.a508.onestep.domain.signal.repository.UserSignalLogRepository;
import com.a508.onestep.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserSignalLogEventListenerTest {

    @Mock private UserSignalLogRepository userSignalLogRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private UserSignalLogEventListener eventListener;

    private static final String TEST_USER_CODE = "TEST_USER_001";
    private static final Long TARGET_ID = 1L;
    private static final LocalDate ASSIGNED_DATE = LocalDate.now();

    @Test
    @DisplayName("이벤트를 수신하고 UserSignalLog를 저장하기 성공")
    void 이벤트를_수신하고_UserSignalLog_저장_성공() {
        // given
        ChallengeCompletedEvent event = createChallengeCompletedEvent(
                TEST_USER_CODE, TARGET_ID,
                Origin.RECOMMENDED, AssignmentStatus.COMPLETED
        );

        // when
        eventListener.handleChallengeCompletedEvent(event);

        // then
        ArgumentCaptor<UserSignalLog> captor = ArgumentCaptor.forClass(UserSignalLog.class);
        verify(userSignalLogRepository).save(captor.capture());

        UserSignalLog savedLog = captor.getValue();
        assertThat(savedLog.getUserCode()).isEqualTo(TEST_USER_CODE);
        assertThat(savedLog.getTargetId()).isEqualTo(TARGET_ID);
        assertThat(savedLog.getEventType()).isEqualTo(Origin.RECOMMENDED);
        assertThat(savedLog.getEventStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("generatedAt 자동 설정 성공")
    void generatedAt_자동_설정_성공() {
        // given
        LocalDateTime before = LocalDateTime.now();
        UserSignalLogEvent event = createUserSignalLogEvent(
                TEST_USER_CODE, TARGET_ID,
                Origin.RECOMMENDED, AssignmentStatus.COMPLETED
        );
        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(event.getGeneratedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("ChallengeCompletedEvent를 from()으로 변환 성공")
    void handleUserSignalLogEvent_FromChallengeCompletedEvent_Success() {
        // given
        User user = User.builder()
                .userCode(TEST_USER_CODE)
                .build();

        ChallengeCompletedEvent challengeEvent = ChallengeCompletedEvent.builder()
                .user(user)
                .challengeId(TARGET_ID)
                .assignedDate(ASSIGNED_DATE)
                .eventType(Origin.RECOMMENDED)
                .eventStatus(AssignmentStatus.COMPLETED)
                .build();

        // when
        UserSignalLogEvent event = UserSignalLogEvent.from(challengeEvent);

        // then
        assertThat(event.getUserCode()).isEqualTo(TEST_USER_CODE);
        assertThat(event.getTargetId()).isEqualTo(TARGET_ID);
        assertThat(event.getAssignedDate()).isEqualTo(ASSIGNED_DATE);
        assertThat(event.getEventType()).isEqualTo(Origin.RECOMMENDED);
        assertThat(event.getEventStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(event.getGeneratedAt()).isNotNull();
    }


    @Test
    @DisplayName("산책 완료 후 저장 성공")
    void 산책_완료_후_저장_성공() {
        // given
        ChallengeCompletedEvent event = createChallengeCompletedEvent(
                TEST_USER_CODE, TARGET_ID, Origin.ROUTE, AssignmentStatus.COMPLETED
        );

        // when
        eventListener.handleChallengeCompletedEvent(event);

        // then
        ArgumentCaptor<UserSignalLog> captor = ArgumentCaptor.forClass(UserSignalLog.class);
        verify(userSignalLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(Origin.ROUTE);
    }

    @Test
    @DisplayName("저장 시 예외가 발생하면 swallow하고 종료")
    void handleChallengeCompletedEvent_SaveFails_DoesNotThrow() {
        // given
        ChallengeCompletedEvent event = createChallengeCompletedEvent(
                TEST_USER_CODE, TARGET_ID, Origin.RECOMMENDED, AssignmentStatus.COMPLETED
        );

        given(userSignalLogRepository.save(any(UserSignalLog.class))).willThrow(new RuntimeException("DB 연결 오류"));

        // when & then
        assertThatCode(() -> eventListener.handleChallengeCompletedEvent(event))
                .doesNotThrowAnyException();

        verify(userSignalLogRepository).save(any(UserSignalLog.class));
    }

    @Test
    @DisplayName("실패: DataAccessException 발생 시도 swallow하고 종료한다")
    void handleChallengeCompletedEvent_DataAccessException_DoesNotThrow() {
        // given
        ChallengeCompletedEvent event = createChallengeCompletedEvent(
                TEST_USER_CODE, TARGET_ID, Origin.RECOMMENDED, AssignmentStatus.COMPLETED
        );

        given(userSignalLogRepository.save(any(UserSignalLog.class)))
                .willThrow(new DataAccessException("저장 실패") {});

        // when & then
        assertThatCode(() -> eventListener.handleChallengeCompletedEvent(event))
                .doesNotThrowAnyException();

        verify(userSignalLogRepository, times(1)).save(any(UserSignalLog.class));
    }

    private ChallengeCompletedEvent createChallengeCompletedEvent(String userCode, Long targetId,
                                                                   Origin eventType, AssignmentStatus eventStatus) {
        User user = User.builder()
                .userCode(userCode)
                .build();

        return ChallengeCompletedEvent.builder()
                .user(user)
                .challengeId(targetId)
                .assignedDate(ASSIGNED_DATE)
                .completedAt(LocalDateTime.now())
                .eventType(eventType)
                .eventStatus(eventStatus)
                .build();
    }

    private UserSignalLogEvent createUserSignalLogEvent(String userCode, Long targetId,
                                           Origin eventType, AssignmentStatus eventStatus) {
        return new UserSignalLogEvent(userCode, targetId, ASSIGNED_DATE, eventType, eventStatus);
    }
}

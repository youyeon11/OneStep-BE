package com.a508.onestep.domain.signal.listener;

import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.signal.event.UserSignalLogEvent;
import com.a508.onestep.domain.signal.repository.UserSignalLogRepository;
import com.a508.onestep.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserSignalLogEventListenerTest {

    @Mock private UserSignalLogRepository userSignalLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private UserSignalLogEventListener eventListener;

    private static final String TEST_USER_CODE = "TEST_USER_001";
    private static final Long TARGET_ID = 1L;
    private static final LocalDate ASSIGNED_DATE = LocalDate.now();

    @BeforeEach
    void setUp() {
        userSignalLogRepository.deleteAll();
    }

    @Test
    @DisplayName("generatedAt 자동 설정 성공")
    void generatedAt_자동_설정_성공() {
        // given
        LocalDateTime before = LocalDateTime.now();
        UserSignalLogEvent event = createUserSignalLogEvent(
                TEST_USER_CODE, TARGET_ID,
                Origin.RECOMMENDED, AssignmentStatus.COMPLETED,
                before, 1
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
    @DisplayName("emotion이 null이어도 이벤트를 생성할 수 있다")
    void emotion_null_허용() {
        // when
        UserSignalLogEvent event = new UserSignalLogEvent(
                TEST_USER_CODE, TARGET_ID, ASSIGNED_DATE,
                Origin.RECOMMENDED, AssignmentStatus.COMPLETED,
                LocalDateTime.now(), null
        );

        // then
        assertThat(event.getEmotion()).isNull();
    }

    @Test
    @DisplayName("completedAt이 null이어도 이벤트를 생성할 수 있다")
    void completedAt_null_허용() {
        // when
        UserSignalLogEvent event = new UserSignalLogEvent(
                TEST_USER_CODE, TARGET_ID, ASSIGNED_DATE,
                Origin.RECOMMENDED, AssignmentStatus.COMPLETED,
                null, 1
        );

        // then
        assertThat(event.getCompletedAt()).isNull();
    }

    private UserSignalLogEvent createUserSignalLogEvent(String userCode, Long targetId,
                                                        Origin eventType, AssignmentStatus eventStatus,
                                                        LocalDateTime completedAt, Integer emotion) {
        return new UserSignalLogEvent(userCode, targetId, ASSIGNED_DATE, eventType, eventStatus, completedAt, emotion);
    }
}
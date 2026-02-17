package com.a508.onestep.domain.signal.event;

import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
비동기 처리용 이벤트
UserSignalLog는 전부 비동기로 보내기
 */
@Getter
@Builder
@AllArgsConstructor
public class UserSignalLogEvent {

    private final String userCode;

    /*
    대상 entity ID(challengeId, routeId 등)
     */
    private final Long targetId;

    private final LocalDate assignedDate;
    private final Origin eventType;
    private final AssignmentStatus eventStatus;
    private final LocalDateTime generatedAt;

    public UserSignalLogEvent(
            String userCode,
            Long targetId,
            LocalDate assignedDate,
            Origin eventType,
            AssignmentStatus eventStatus
    ) {
        this.userCode = userCode;
        this.targetId = targetId;
        this.assignedDate = assignedDate;
        this.eventType = eventType;
        this.eventStatus = eventStatus;
        this.generatedAt = LocalDateTime.now();
    }

    /**
     * 동기 이벤트를 비동기 이벤트로 변환
     *
     * @param event 원본 챌린지 완료 이벤트
     * @return 비동기 처리용 이벤트
     */
    public static UserSignalLogEvent from(ChallengeCompletedEvent event) {

        return new UserSignalLogEvent(
                event.getUser().getUserCode(),
                event.getChallengeId(),
                event.getAssignedDate(),
                event.getEventType(),
                event.getEventStatus()
        );
    }
}

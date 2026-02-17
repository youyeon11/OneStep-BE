package com.a508.onestep.domain.challenge.event;

import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.letter.entity.Letter;
import com.a508.onestep.domain.route.entity.RouteSession;
import com.a508.onestep.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
챌린지 완료
 */
@Getter
@Builder
public class ChallengeCompletedEvent {
    private final User user;
    private final Long challengeId;
    private final LocalDate assignedDate;
    private final Integer earnedExp;
    private final Integer emotion;
    private final LocalDateTime completedAt;
    private final Origin eventType;
    private final AssignmentStatus eventStatus;

    public static ChallengeCompletedEvent fromChallengeAssignment(ChallengeAssignment challenge) {
        return ChallengeCompletedEvent.builder()
                .user(challenge.getUser())
                .challengeId(challenge.getId())
                .assignedDate(challenge.getAssignedDate())
                .earnedExp(challenge.getExp())
                .emotion(challenge.getEmotion())
                .completedAt(LocalDateTime.now())
                .eventType(challenge.getOrigin())
                .eventStatus(challenge.getChallengeStatus())
                .build();
    }

    public static ChallengeCompletedEvent fromRouteSession(RouteSession routeSession) {
        return ChallengeCompletedEvent.builder()
                .user(routeSession.getUser())
                .challengeId(routeSession.getId())
                .assignedDate(LocalDate.now())
                .earnedExp(40)
                .completedAt(LocalDateTime.now())
                .eventType(Origin.ROUTE)
                .eventStatus(routeSession.getRouteStatus())
                .build();
    }

    public static ChallengeCompletedEvent fromLetter(Letter letter) {
        return ChallengeCompletedEvent.builder()
                .user(letter.getUser())
                .challengeId(letter.getId())
                .assignedDate(LocalDate.now())
                .earnedExp(15)
                .completedAt(LocalDateTime.now())
                .eventType(Origin.LETTER)
                .eventStatus(AssignmentStatus.COMPLETED)
                .build();
    }
}

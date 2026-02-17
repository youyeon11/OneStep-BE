package com.a508.onestep.domain.challenge.entity;

import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "challenge_assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChallengeAssignment extends BaseTimeEntity {

        @Id @Column(name = "id")
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "assigned_date", nullable = false)
        private LocalDate assignedDate;

        @Enumerated(EnumType.STRING)
        @Column(name = "challenge_status", nullable = false)
        private AssignmentStatus challengeStatus;

        @Column(name = "content")
        private String content;

        @Enumerated(EnumType.STRING)
        @Column(name = "origin", nullable = false)
        private Origin origin;

        @Column(name = "log_date")
        private LocalDate logDate;

        @Column(name = "emotion", nullable = true)
        private Integer emotion;

        @Column(name = "exp", nullable = true)
        private Integer exp;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private User user;

        @Column(name = "challenge_master_id")
        private Long challengeMasterId;

        @Column(name = "completed_at")
        private LocalDateTime completedAt;

        // 완료 처리하기
        public void complete(Integer emotion) {
                this.challengeStatus = AssignmentStatus.COMPLETED;
                this.logDate = LocalDate.now();
                this.emotion = emotion;
                this.completedAt = LocalDateTime.now();
        }
}

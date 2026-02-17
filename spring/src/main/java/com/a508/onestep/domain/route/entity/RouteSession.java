package com.a508.onestep.domain.route.entity;

import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "route_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class RouteSession extends BaseTimeEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Enumerated(EnumType.STRING)
        @Column(name = "route_status", nullable = false)
        private AssignmentStatus routeStatus;

        @Column(name = "started_at")
        private LocalDateTime startedAt;

        @Column(name = "ended_at")
        private LocalDateTime endedAt; // completedAt

        @Column(name = "duration_seconds")
        private Integer durationSeconds;

        @Column(name = "destination_grid")
        private String destinationGrid;

        @Column(name = "log_date")
        private LocalDate logDate;

        @Column(name = "content")
        private String content;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private User user;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "route_levels_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private RouteLevel routeLevel;

        @Column(name = "image_url")
        private String imageUrl;

        /**
         * 경로 시작
         */
        public void start() {
                this.routeStatus = AssignmentStatus.PROGRESS;
                this.startedAt = LocalDateTime.now();
        }

        /**
         * 경로 완료
         */
        public void complete() {
                this.routeStatus = AssignmentStatus.COMPLETED;
                this.endedAt = LocalDateTime.now();
                if (this.startedAt != null) {
                        this.durationSeconds = (int) Duration.between(startedAt, endedAt).getSeconds();
                }
        }

        /**
         * 경로 취소
         */
        public void cancel() {
                this.routeStatus = AssignmentStatus.CANCELED;
                this.endedAt = LocalDateTime.now();
                if (this.startedAt != null) {
                        this.durationSeconds = (int) Duration.between(startedAt, endedAt).getSeconds();
                }
        }

        /**
         * url 추가
         */
        public void saveUrl(String url) {
                this.imageUrl = url;
        }
}

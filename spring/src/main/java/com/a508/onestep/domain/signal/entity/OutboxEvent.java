package com.a508.onestep.domain.signal.entity;

import com.a508.onestep.domain.common.AggregateType;
import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.common.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 아웃박스 패턴 이벤트 저장 엔티티
 */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OutboxEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이벤트를 발생시킨 도메인 애그리게이트 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type")
    private AggregateType aggregateType;

    // 이벤트를 발생시킨 애그리게이트 식별자
    @Column(name = "aggregate_id")
    private Long aggregateId;

    // 발생한 이벤트의 이름
    @Column(name = "event_type", nullable = false)
    private String eventType;

    // json으로 직렬화된 페이로드
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    // 발행 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "outbox_status", nullable = false)
    @Builder.Default
    private OutboxStatus outboxStatus = OutboxStatus.PENDING;

    // kafka 발행 재시도 횟수
    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    // 마지막 실패 원인(성공 시 null)
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // Kafka 발행 완료 시각 (미발행 시 null)
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /*
    전송 완료 상태로 변경
     */
    public void markSent() {
        this.outboxStatus = OutboxStatus.SENT;
        this.publishedAt = LocalDateTime.now();
    }

    /*
    전송 실패
     */
    public void markFailed(String errorMessage) {
        this.outboxStatus = OutboxStatus.FAILED;
        this.retryCount++;
        this.errorMessage = errorMessage;
    }

    /*
    다시 전송 중의 상태
     */
    public void resetToPending() {
        this.outboxStatus = OutboxStatus.PENDING;
    }


    /*
    Outbox 생성
     */
    public static OutboxEvent create(AggregateType aggregateType, Long aggregateId, String eventType, String payload) {
        return OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .outboxStatus(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }
}

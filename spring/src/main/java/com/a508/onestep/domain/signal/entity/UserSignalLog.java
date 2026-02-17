package com.a508.onestep.domain.signal.entity;

import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.common.Origin;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "user_signal_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSignalLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private Origin eventType;

    @Column(name = "target_id")
    private Long targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status")
    private AssignmentStatus eventStatus;

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "user_code")
    private String userCode;

    /*
    디버깅
     */
    @Override
    public String toString() {
        return "UserSignalLog{" +
                "logId=" + logId +
                ", eventType=" + eventType +
                ", targetId=" + targetId +
                ", metadata='" + metadata + '\'' +
                ", eventStatus=" + eventStatus +
                ", weight=" + weight +
                ", userCode='" + userCode + '\'' +
                '}';
    }
}
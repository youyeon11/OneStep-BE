package com.a508.onestep.domain.signal.entity;

import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.user.entity.User;
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

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@Table(name = "user_interest_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserInterestProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> keywords;

    @Column(name = "last_analyzed_at")
    private LocalDateTime lastAnalyzedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;
}

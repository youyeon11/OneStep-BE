package com.a508.onestep.domain.letter.entity;

import  com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "letters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Letter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_status", nullable = false)
    private FilterStatus filterStatus;

    public void changeFilterStatus(FilterStatus status) {
        this.filterStatus = status;
    }

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "title")
    private String title;
}

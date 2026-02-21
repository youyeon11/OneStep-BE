package com.a508.onestep.domain.challenge.entity;

import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.common.TagCategory;
import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Entity;

@Entity
@Table(name = "challenge_master")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChallengeMaster extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_master_id")
    private Long id;

    @Column(length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, name = "category")
    private TagCategory category;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel;

    private Integer reward;

    @Column(columnDefinition = "TEXT")
    private String tags;
}

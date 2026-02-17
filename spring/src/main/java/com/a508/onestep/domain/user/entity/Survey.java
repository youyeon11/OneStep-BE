package com.a508.onestep.domain.user.entity;

import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.common.TagCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "survey")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Survey extends BaseTimeEntity {

    @Id @GeneratedValue(strategy =  GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "survey_number")
    private Integer surveyNumber;

    @Column(name = "content")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private TagCategory category;  // INNER, LIFESTYLE, SOCIAL
}

package com.a508.onestep.domain.user.entity;

import com.a508.onestep.domain.common.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "survey_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SurveyLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "answer")
    private Integer answer;

    @Column(name = "survey_number")
    private Integer surveyNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    /*
     생성자
     */
    public SurveyLog(User user, Integer surveyNumber, Integer answer) {
        this.user = user;
        this.surveyNumber = surveyNumber;
        this.answer = answer;
    }

    /*
     답변 기록 업데이트
     */
    public void updateAnswer(Integer newAnswer) {
        if (newAnswer == null || newAnswer < 0 || newAnswer > 3) {
            throw new IllegalArgumentException("답변은 0~3 사이의 값이어야 합니다.");
        }
        this.answer = newAnswer;
    }
}

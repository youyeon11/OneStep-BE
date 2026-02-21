package com.a508.onestep.domain.challenge.repository;

import com.a508.onestep.domain.challenge.entity.ChallengeMaster;
import com.a508.onestep.domain.challenge.entity.QChallengeMaster;
import com.a508.onestep.domain.common.TagCategory;
import com.a508.onestep.domain.user.entity.QSurvey;
import com.a508.onestep.domain.user.entity.QSurveyLog;
import com.a508.onestep.domain.user.entity.User;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ChallengeMasterRepositoryCustomImpl implements ChallengeMasterRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final QChallengeMaster challengeMaster = QChallengeMaster.challengeMaster;

    @Override
    public List<ChallengeMaster> findRecommendedChallenges(
            Integer difficultyLevel, List<TagCategory> categorieList, User user
    ) {
        // EXISTS 서브쿼리
        QSurveyLog surveyLog = QSurveyLog.surveyLog;

        boolean hasSurveyLog = jpaQueryFactory
                .select(Expressions.ONE)
                .from(surveyLog)
                .where(surveyLog.user.eq(user))
                .fetchFirst() != null;

        // surveyLog 없을 때 DB에서 랜덤 정렬
        if (!hasSurveyLog) {
            return jpaQueryFactory
                    .selectFrom(challengeMaster)
                    .where(
                            challengeMaster.difficultyLevel.eq(difficultyLevel),
                            challengeMaster.category.in(categorieList)
                    )
                    .orderBy(Expressions.numberTemplate(Double.class, "RANDOM()").asc())
                    .limit(20)
                    .fetch();
        }

        // surveyLog 있을 때
        QSurvey survey = QSurvey.survey;

        return jpaQueryFactory
                .select(challengeMaster)
                .from(challengeMaster)
                .join(survey)
                .on(survey.category.eq(challengeMaster.category))
                .join(surveyLog)
                .on(
                        surveyLog.surveyNumber.eq(survey.surveyNumber),
                        surveyLog.user.eq(user)
                )
                .where(
                        challengeMaster.difficultyLevel.eq(difficultyLevel),
                        challengeMaster.category.in(categorieList)
                )
                .groupBy(challengeMaster.id)
                .orderBy(surveyLog.answer.sum().desc())
                .limit(20)
                .fetch();
    }
}

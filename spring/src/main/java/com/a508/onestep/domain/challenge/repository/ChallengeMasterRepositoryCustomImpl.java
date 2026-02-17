package com.a508.onestep.domain.challenge.repository;

import com.a508.onestep.domain.challenge.entity.ChallengeMaster;
import com.a508.onestep.domain.challenge.entity.QChallengeMaster;
import com.a508.onestep.domain.common.TagCategory;
import com.a508.onestep.domain.user.entity.QSurvey;
import com.a508.onestep.domain.user.entity.QSurveyLog;
import com.a508.onestep.domain.user.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ChallengeMasterRepositoryCustomImpl implements ChallengeMasterRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final QChallengeMaster challengeMaster = QChallengeMaster.challengeMaster;

    @Override
    public List<ChallengeMaster> findRecommendedChallenges(
            Integer difficultyLevel, List<TagCategory> categorieList, User user
    ) {
        QSurvey survey = QSurvey.survey;
        QSurveyLog surveyLog = QSurveyLog.surveyLog;

        List<String> categoryNames = categorieList.stream()
                .map(Enum::name)
                .toList();

        boolean hasSurveyLog = jpaQueryFactory
                .selectOne()
                .from(surveyLog)
                .where(surveyLog.user.eq(user))
                .fetchFirst() != null;

        // surveyLog가 없을 때
        if (!hasSurveyLog) {
            List<ChallengeMaster> results = jpaQueryFactory
                    .selectFrom(challengeMaster)
                    .where(
                            challengeMaster.difficultyLevel.eq(difficultyLevel),
                            challengeMaster.category.in(categoryNames)
                    )
                    .fetch();

            Collections.shuffle(results);
            return results;
        }

        return jpaQueryFactory
                .select(challengeMaster)
                .from(challengeMaster)
                .join(survey)
                .on(survey.category.stringValue().eq(challengeMaster.category))
                .join(surveyLog)
                .on(
                        surveyLog.surveyNumber.eq(survey.surveyNumber),
                        surveyLog.user.eq(user)
                )
                .where(
                        challengeMaster.difficultyLevel.eq(difficultyLevel),
                        challengeMaster.category.in(categoryNames)
                )
                .groupBy(challengeMaster.id)
                .orderBy(surveyLog.answer.sum().desc())
                .fetch();
    }
}

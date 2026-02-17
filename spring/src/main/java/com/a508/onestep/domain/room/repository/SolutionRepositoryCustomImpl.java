package com.a508.onestep.domain.room.repository;

import com.a508.onestep.domain.room.entity.QSolution;
import com.a508.onestep.domain.room.entity.QTrouble;
import com.a508.onestep.domain.room.entity.Solution;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SolutionRepositoryCustomImpl implements SolutionRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final QSolution solution = QSolution.solution;
    private final QTrouble trouble = QTrouble.trouble;

    @Override
    public List<Solution> findSolutionsByUserCode(String userCode) {
        return jpaQueryFactory
                .selectFrom(solution)
                .join(trouble).on(solution.troubleId.eq(trouble.id))
                .where(trouble.userCode.eq(userCode))
                .fetch();
    }
}

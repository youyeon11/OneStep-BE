package com.a508.onestep.domain.room.repository;

import com.a508.onestep.domain.room.entity.Solution;

import java.util.List;

public interface SolutionRepositoryCustom {

    List<Solution> findSolutionsByUserCode(String userCode);
}

package com.a508.onestep.domain.room.repository;

import com.a508.onestep.domain.room.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Long>, SolutionRepositoryCustom {
}

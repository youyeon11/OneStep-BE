package com.a508.onestep.domain.challenge.repository;


import com.a508.onestep.domain.challenge.entity.RecommendedRoutine;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendedRoutineRepository extends MongoRepository<RecommendedRoutine, String> {

    Optional<RecommendedRoutine> findByUserCode(String userCode);
}
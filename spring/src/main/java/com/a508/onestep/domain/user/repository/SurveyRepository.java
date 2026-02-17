package com.a508.onestep.domain.user.repository;

import com.a508.onestep.domain.user.entity.SurveyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<SurveyLog, Long> {

    @Query("""
    SELECT sl FROM SurveyLog sl
    WHERE sl.user.id = :userId AND sl.surveyNumber = :surveyNumber
    """)
    Optional<SurveyLog> findBySurveyNumberAndUser(
            @Param("surveyNumber") Integer surveyNumber,
            @Param("userId") Long userId
    );

    @Query("""
    SELECT sl FROM SurveyLog sl
    WHERE sl.user.id = :userId
    """)
    List<SurveyLog> findByUserId(@Param("userId") Long userId);
}

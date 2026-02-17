package com.a508.onestep.domain.user.repository;

import com.a508.onestep.domain.common.TagCategory;
import com.a508.onestep.domain.user.entity.SurveyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyLogRepository extends JpaRepository<SurveyLog, Long> {

    @Query("""
            SELECT DISTINCT s.category
            FROM SurveyLog sl
            JOIN Survey s ON sl.surveyNumber = s.surveyNumber
            WHERE sl.user.id = :userId
            """)
    List<TagCategory> findCategoriesByUserId(@Param("userId") Long userId);
}

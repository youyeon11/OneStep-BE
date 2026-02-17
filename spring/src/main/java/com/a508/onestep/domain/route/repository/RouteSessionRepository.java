package com.a508.onestep.domain.route.repository;

import com.a508.onestep.domain.route.entity.RouteSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RouteSessionRepository extends JpaRepository<RouteSession, Long> {

    @Query("""
    select rs from RouteSession rs where rs.user.id = :userId and rs.logDate = :logDate
    """)
    Optional<RouteSession> findByUserIdAndLogDate(@Param("userId") Long userId, @Param("logDate") LocalDate logDate);
}

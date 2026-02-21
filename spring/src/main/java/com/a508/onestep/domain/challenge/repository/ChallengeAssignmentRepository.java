package com.a508.onestep.domain.challenge.repository;

import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeAssignmentRepository extends JpaRepository<ChallengeAssignment, Long> {

    @Query("""
        SELECT c
        FROM ChallengeAssignment c
        WHERE c.user.id = :userId
            AND c.challengeStatus = :challengeStatus
            AND c.logDate BETWEEN :startDate AND :endDate
    """)
    List<ChallengeAssignment> findByUserAndLogDateBetweenAndCompleted(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            AssignmentStatus challengeStatus
    );

    @Query("""
        SELECT c
        FROM ChallengeAssignment c
        WHERE c.user.id = :userId
            AND c.logDate = :logDate
            AND c.challengeStatus = :challengeStatus
    """)
    List<ChallengeAssignment> findByUserAndLogDateAndCompleted(
            Long userId,
            LocalDate logDate,
            AssignmentStatus challengeStatus
    );

    /*
    특정 사용자의 ChallengeAssignment 전부 조회
     */
    @Query("""
    SELECT c
    FROM ChallengeAssignment c
    WHERE c.user.id = :userId
      AND c.assignedDate = :assignedDate AND c.origin = :origin
    """)
    List<ChallengeAssignment> findSelfByUserIdAndAssignedDate(
            @Param("userId") Long userId,
            @Param("assignedDate") LocalDate assignedDate,
            @Param("origin") Origin origin
    );

    @Query("SELECT ca FROM ChallengeAssignment ca " +
            "JOIN ca.user u " +
            "WHERE u.userCode = :userCode " +
            "AND ca.assignedDate = :assignedDate " +
            "AND ca.origin = :origin")
    List<ChallengeAssignment> findByUserCodeAndAssignedDateAndOrigin(
            @Param("userCode") String userCode,
            @Param("assignedDate") LocalDate assignedDate,
            @Param("origin") Origin origin
    );

    @Query("SELECT ca FROM ChallengeAssignment ca " +
            "JOIN FETCH ca.user " +
            "WHERE ca.id = :id")
    Optional<ChallengeAssignment> findByIdWithUser(@Param("id") Long id);
}
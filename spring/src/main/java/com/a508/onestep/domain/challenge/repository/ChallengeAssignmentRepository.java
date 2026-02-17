package com.a508.onestep.domain.challenge.repository;

import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

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

    /*
    Tag에 대하면 있으면 수집하는 쿼리
     */
    @Query("""
            SELECT cm.tags
            FROM ChallengeAssignment ca
            JOIN ChallengeMaster cm ON ca.challengeMasterId = cm.id
            WHERE ca.user.id = :userId
              AND ca.challengeMasterId IS NOT NULL
            """)
    List<String> findChallengeMasterTagsByUserId(@Param("userId") Long userId);
}

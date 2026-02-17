package com.a508.onestep.domain.signal.repository;

import com.a508.onestep.domain.signal.entity.UserSignalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserSignalLogRepository extends JpaRepository<UserSignalLog, Long> {

    /**
     * @param userCodes : userCode
     * @param startDate : 하루의 시작(LocalDateTime)
     * @param endDate : 하루의 마지막(LocalDateTime)
     * @return 해당 시간 내의 모든 UserSignalLog
     */
    @Query("SELECT u FROM UserSignalLog u WHERE u.userCode IN :userCodes AND u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.userCode, u.createdAt DESC")
    List<UserSignalLog> findByUserCodeInAndCreatedAtBetween(
            @Param("userCodes") List<String> userCodes,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}

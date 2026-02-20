package com.a508.onestep.domain.letter.repository;

import com.a508.onestep.domain.letter.entity.LetterDelivery;
import com.a508.onestep.domain.letter.entity.StorageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LetterDeliveryRepository extends JpaRepository<LetterDelivery, Long> {

    // 수신함(UNSAVED) / 보관함(SAVED) 목록 조회
    @Query("""
    SELECT ld
    FROM LetterDelivery ld
    JOIN FETCH ld.letter l
    WHERE ld.receiver.id = :receiverId
        AND ld.storageStatus = :storageStatus
    ORDER BY ld.deliveredAt DESC
    """)
    List<LetterDelivery> findByReceiverIdAndStorageStatusWithLetter(
            @Param("receiverId") Long receiverId,
            @Param("storageStatus") StorageStatus storageStatus
    );

    /**
     * 오늘 수신한 편지 조회
     */
    @Query("""
        SELECT ld
        FROM LetterDelivery ld
        WHERE ld.receiver.id = :receiverId
            AND ld.deliveredAt BETWEEN :startOfDay AND :endOfDay
        """)
    Optional<LetterDelivery> findTodayDelivery(
            @Param("receiverId") Long receiverId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // 보관함(SAVED) 상세 조회 + 권한 검증
    Optional<LetterDelivery> findByReceiverIdAndLetterIdAndStorageStatus(
            Long receiverId,
            Long letterId,
            StorageStatus storageStatus
    );

    // 저장/삭제 상태 변경용 조회 (UNSAVED/SAVED/DELETED 모두 가능)
    Optional<LetterDelivery> findByReceiverIdAndLetterId(
            Long receiverId,
            Long letterId
    );
}

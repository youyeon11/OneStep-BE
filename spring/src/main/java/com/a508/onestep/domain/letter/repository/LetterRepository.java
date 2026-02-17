package com.a508.onestep.domain.letter.repository;

import com.a508.onestep.domain.letter.entity.Letter;
import com.a508.onestep.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LetterRepository extends JpaRepository<Letter, Long> {

    /**
     * 하루 1회 작성 제한 체크
     *
     * @param user  작성자
     * @param start 오늘 시작 시간 (00:00:00)
     * @param end   오늘 종료 시간 (23:59:59)
     * @return 작성 데이터 존재 여부
     */
    boolean existsByUserAndCreatedAtBetween(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 수신 가능한 편지 ID 목록 조회
     */
    @Query(value = """
        SELECT l.id
        FROM letters l
        WHERE l.user_id != :userId
            AND l.filter_status = 'PASS'
            AND NOT EXISTS (
                SELECT 1
                FROM letter_deliveries ld
                WHERE ld.letter_id = l.id
                    AND ld.receiver_id = :userId
            )
        """, nativeQuery = true)
    List<Long> findCandidateLetterIds(@Param("userId") Long userId);
}
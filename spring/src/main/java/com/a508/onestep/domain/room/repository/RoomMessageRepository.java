package com.a508.onestep.domain.room.repository;

import com.a508.onestep.domain.room.entity.RoomMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomMessageRepository extends JpaRepository<RoomMessage, Long> {

    /**
     * 특정 방의 메시지 조회 (페이징)
     */
    Page<RoomMessage> findByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    /**
     * 특정 방의 메시지 조회 (커서 기반 페이징)
     */
    @Query("SELECT rm FROM RoomMessage rm " +
            "WHERE rm.roomId = :roomId " +
            "AND rm.isDeleted = false " +
            "AND rm.id < :cursorId " +
            "ORDER BY rm.id DESC")
    List<RoomMessage> findByRoomIdWithCursor(@Param("roomId") Long roomId, @Param("cursorId") Long cursorId, Pageable pageable);

    /**
     * 특정 방의 최신 N개 메시지 조회
     */
    List<RoomMessage> findTop50ByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(Long roomId);

    /**
     * 특정 방의 메시지 개수
     */
    long countByRoomIdAndIsDeletedFalse(Long roomId);

    /**
     * 특정 시간 이후 메시지 조회
     */
    List<RoomMessage> findByRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtAsc(Long roomId, LocalDateTime after);

    /**
     * 특정 방의 모든 메시지 조회 (시간순)
     */
    List<RoomMessage> findByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(Long roomId);
}

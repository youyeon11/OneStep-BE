package com.a508.onestep.domain.room.repository;

import com.a508.onestep.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findById(Long id);

    @Query("SELECT r FROM Room r WHERE r.closedAt IS NULL OR r.closedAt > :now")
    List<Room> findActiveRooms(@Param("now") LocalDateTime now);


    @Query("""
        SELECT r
        FROM Room r
        LEFT JOIN Trouble t ON r.troubleId = t.id
        WHERE t.userCode != :userCode
            AND r.closedAt is null
    """)
    List<Room> findByTroubleIdIsNotEqualUserCodeANDCLOSEDATISNULL(String userCode);
}

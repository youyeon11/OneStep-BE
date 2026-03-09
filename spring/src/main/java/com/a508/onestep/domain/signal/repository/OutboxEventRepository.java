package com.a508.onestep.domain.signal.repository;

import com.a508.onestep.domain.signal.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // FOR UPDATE SKIP LOCKED를 통한 스킵
    @Query(value = """
        SELECT *
        FROM outbox_events
        WHERE outbox_status = 'PENDING'
        ORDER BY id
        FOR UPDATE SKIP LOCKED
        LIMIT :batchSize
    """, nativeQuery = true)
    List<OutboxEvent> findPendingForUpdate(@Param("batchSize") int batchSize);
}

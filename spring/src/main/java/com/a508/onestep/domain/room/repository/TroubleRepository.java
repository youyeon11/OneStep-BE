package com.a508.onestep.domain.room.repository;

import com.a508.onestep.domain.room.entity.Trouble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TroubleRepository extends JpaRepository<Trouble, Long> {

    List<Trouble> findByUserCode(String userCode);

    @Query(value = """
            SELECT *
            FROM trouble AS t
            WHERE t.user_code != :userCode
            ORDER BY RANDOM()
            LIMIT 1
            """, nativeQuery = true)
    Optional<Trouble> findRandomAndNotEqualUserCode(String userCode);
}

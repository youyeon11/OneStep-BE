package com.a508.onestep.domain.challenge.repository;

import com.a508.onestep.domain.challenge.entity.ChallengeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

@Repository
public interface ChallengeMasterRepository extends JpaRepository<ChallengeMaster, Long>, ChallengeMasterRepositoryCustom {
    @Query("""
            SELECT c
            FROM ChallengeMaster c
            WHERE c.id = :challengeId
            """)
    Optional<ChallengeMaster> findById(Long id);
}

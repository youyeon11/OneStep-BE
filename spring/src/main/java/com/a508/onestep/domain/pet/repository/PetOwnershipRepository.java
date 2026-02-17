package com.a508.onestep.domain.pet.repository;

import com.a508.onestep.domain.pet.entity.PetOwnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetOwnershipRepository extends JpaRepository<PetOwnership, Long> {

    boolean existsByUserId(Long userId);
    Optional<PetOwnership> findByUserId(long userId);
}

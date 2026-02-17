package com.a508.onestep.domain.route.repository;

import com.a508.onestep.domain.route.entity.RouteLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RouteLevelRepository extends JpaRepository<RouteLevel, Long> {

    /*
    루트 레벨에 대하여 RouteLevel을 조회
    */
    @Query("select r from RouteLevel r where r.routeLevel = :routeLevel")
    Optional<RouteLevel> findByRouteLevel(@Param("routeLevel") int routeLevel);
}

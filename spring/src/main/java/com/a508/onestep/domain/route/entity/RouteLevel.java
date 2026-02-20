package com.a508.onestep.domain.route.entity;

import com.a508.onestep.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "route_levels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RouteLevel extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_level")
    private Integer routeLevel;

    @Column(nullable = false)
    private String label;

    @Column(name = "recommended_min_distance_m")
    private Integer recommendedMinDistanceM;

    @Column(name = "recommended_max_distance_m")
    private Integer recommendedMaxDistanceM;
}

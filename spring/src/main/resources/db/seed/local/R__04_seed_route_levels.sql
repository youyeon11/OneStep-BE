-- Level 1: 가벼운 산책 (짧은 거리)
INSERT INTO route_levels (route_level, label, recommended_min_distance_m, recommended_max_distance_m)
VALUES (1, '집 안 산책', 0, 0);

-- Level 2: 보통 산책 (중간 거리)
INSERT INTO route_levels (route_level, label, recommended_min_distance_m, recommended_max_distance_m)
VALUES (2, '가벼운 산책', 0, 0);

-- Level 3: 긴 산책 (장거리)
INSERT INTO route_levels (route_level, label, recommended_min_distance_m, recommended_max_distance_m)
VALUES (3,'충분한 산책', 500, 1000);

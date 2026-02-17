package com.a508.onestep.domain.route.service;

import com.a508.onestep.domain.route.dto.request.RouteLocationRequestDto;
import com.a508.onestep.domain.route.dto.response.RouteChallengeResponseDto;
import com.a508.onestep.domain.route.dto.response.RouteDetailResponseDto;
import com.a508.onestep.domain.route.dto.response.RouteRecommendResponseDto;

public interface RouteService {

    RouteRecommendResponseDto getRecommendation(RouteLocationRequestDto requestDto);

    RouteDetailResponseDto getDetailRoute(Long routeId, RouteLocationRequestDto requestDto);

    RouteChallengeResponseDto start(Long routeId);

    RouteChallengeResponseDto check(Long routeId);

    RouteChallengeResponseDto end(Long routeId, RouteLocationRequestDto requestDto);
}

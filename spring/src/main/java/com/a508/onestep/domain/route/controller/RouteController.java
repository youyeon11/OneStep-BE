package com.a508.onestep.domain.route.controller;

import com.a508.onestep.domain.route.dto.request.RouteLocationRequestDto;
import com.a508.onestep.domain.route.dto.response.RouteChallengeResponseDto;
import com.a508.onestep.domain.route.dto.response.RouteDetailResponseDto;
import com.a508.onestep.domain.route.dto.response.RouteRecommendResponseDto;
import com.a508.onestep.domain.route.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Route Controller", description = "산책 경로 관련 API")
public class RouteController {

    private final RouteService routeService;

    @PostMapping("")
    @Operation(
            summary = "경로 추천 조회하기",
            description = """
                    사용자의 현재 위치(위도, 경도)를 기반으로 맞춤형 산책 경로를 추천합니다.
                    RecoveryLevel이 3인 사용자에게만 추천 리스트가 반환됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "경로 추천 성공",
                    content = @Content(schema = @Schema(implementation = RouteRecommendResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (위도/경도 누락 또는 범위 초과)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    public RouteRecommendResponseDto recommend(
            @Parameter(description = "사용자 위치 정보", required = true)
            @RequestBody RouteLocationRequestDto requestDto) {
        return routeService.getRecommendation(requestDto);
    }

    @PostMapping("/{routeId}")
    @Operation(
            summary = "산책 루틴 상세 조회",
            description = "추천받은 산책 루틴의 상세 정보를 조회합니다. 예상 소요 시간, 경로 상세 내용 등을 포함합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "상세 조회 성공",
            content = @Content(schema = @Schema(implementation = RouteDetailResponseDto.class))
    )
    public RouteDetailResponseDto detail(
            @PathVariable("routeId") Long routeId,
            @RequestBody RouteLocationRequestDto requestDto
    ) {
        return routeService.getDetailRoute(routeId, requestDto);
    }

    @PostMapping("/{routeId}/start")
    @Operation(
            summary = "산책 챌린지 시작",
            description = """
    선택한 산책 경로에 대한 챌린지를 시작합니다. 새로운 세션이 생성되고 시작 시간이 기록됩니다.<br>
    시작하고 싶은 산책 경로에 대한 ID(PK)값을 넣어주세요.
    """)
    @ApiResponse(
            responseCode = "200",
            description = "챌린지 시작 성공",
            content = @Content(schema = @Schema(implementation = RouteChallengeResponseDto.class))
    )
    public RouteChallengeResponseDto start(
            @PathVariable("routeId") Long routeId
    ) {
        return routeService.start(routeId);
    }

    @PostMapping("/{routeId}/complete")
    @Operation(
            summary = "산책 챌린지 종료",
            description = "진행 중인 산책 챌린지를 완료 처리합니다. 완료 시간이 기록되고 상태가 변경됩니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "챌린지 종료 성공",
            content = @Content(schema = @Schema(implementation = RouteChallengeResponseDto.class))
    )
    public RouteChallengeResponseDto complete(
            @PathVariable("routeId") Long routeId,
            @RequestBody RouteLocationRequestDto requestDto
    ) {
        return routeService.end(routeId, requestDto);
    }

    @GetMapping("/{routeId}/session")
    @Operation(
            summary = "진행 중인 산책 세션 조회",
            description = "현재 진행 중인 산책 세션의 상태를 조회합니다. 세션 ID, 시작 시간, 현재 상태 등을 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "세션 조회 성공",
            content = @Content(schema = @Schema(implementation = RouteChallengeResponseDto.class))
    )
    public RouteChallengeResponseDto getSession(
            @PathVariable("routeId") Long routeId) {
        return routeService.check(routeId);
    }
}

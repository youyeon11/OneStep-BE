package com.a508.onestep.domain.challenge.controller;

import com.a508.onestep.domain.challenge.dto.request.ChallengeCompleteRequestDto;
import com.a508.onestep.domain.challenge.dto.request.ChallengeRequestDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeCompleteResponseDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeResponseDto;
import com.a508.onestep.domain.challenge.dto.response.InitialChallengeResponseDto;
import com.a508.onestep.domain.challenge.service.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
@Tag(name = "Challenge API Controller", description = "챌린지(할 일) 관련 API Controller")
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping("")
    @Operation(summary = "나의 챌린지 등록하기", description = """
            사용자가 추천 이외에 스스로 등록하는 챌린지(할 일)입니다.
            """)
    public ChallengeResponseDto register(
            @RequestBody ChallengeRequestDto requestDto
            ) {
        return challengeService.register(requestDto);
    }

    @GetMapping("")
    @Operation(summary = "나의 챌린지 조회하기", description = """
            사용자가 스스로 등록한 챌린지를 조회합니다. <br>
            내가 등록한 것에 대해서만 반환됩니다.
            """)
    public List<ChallengeResponseDto> getAll() {
        return challengeService.getAll();
    }

    @PostMapping("/complete")
    @Operation(summary = "챌린지 완료하기", description = "챌린지 완료하기")
    public ChallengeCompleteResponseDto complete(
            @RequestBody ChallengeCompleteRequestDto requestDto
    ) {
        return challengeService.complete(requestDto);
    }

    @GetMapping("/init")
    @Operation(summary = "초기 유저에게 20개 추천하기", description = """
    초기 유저에 대하여 20개의 챌린지를 추천합니다. 해당 챌린지는 설문조사 기반의 레벨을 기반으로 처리됩니다.<br>
    여기서 5개를 생성해주시면 됩니다.
    """)
    public List<InitialChallengeResponseDto> getRecommendations() {
        return challengeService.getInitialRecommendations();
    }

    @PostMapping("/init/choices")
    @Operation(summary = "초기 유저에게 5개 선택하기", description = """
    초기 유저가 추천받은 20개 중 5개를 한 번에 자신의 챌린지(할 일)로 저장합니다.<br>
    5개가 아니어도 됩니다.
    """)
    public List<ChallengeResponseDto> select(
            @RequestBody List<ChallengeRequestDto> requestDto
    ) {
        return challengeService.selectInitialChallenges(requestDto);
    }

    @GetMapping("/recommend")
    @Operation(summary = "추천 챌린지 조회하기", description = "추천 챌린지들만 조회합니다.")
    public List<ChallengeResponseDto> getRecommendedChallenge() {
        /** 현재 로그인한 사용자의 일일 추천 챌린지를
         * 몽고 DB에서 반환하는 메서드
         *
         */
        return challengeService.getChallenge();
    }
}

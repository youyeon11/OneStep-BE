package com.a508.onestep.domain.challenge.service;

import com.a508.onestep.domain.challenge.dto.request.ChallengeCompleteRequestDto;
import com.a508.onestep.domain.challenge.dto.request.ChallengeRequestDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeCompleteResponseDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeResponseDto;
import com.a508.onestep.domain.challenge.dto.response.InitialChallengeResponseDto;

import java.util.List;

public interface ChallengeService {
    // 챌린지 가져오기
    public List<ChallengeResponseDto> getChallenge();

    // 챌린지 등록하기
    ChallengeResponseDto register(ChallengeRequestDto requestDto);

    // 챌린지 조회하기
    List<ChallengeResponseDto> getAll();

    // 챌린지 완료하기
    ChallengeCompleteResponseDto complete(ChallengeCompleteRequestDto requestDto);

    // 초기 유저에게 챌린지 20개 제시
    List<InitialChallengeResponseDto> getInitialRecommendations();

    // 5개의 챌린지 저장하기
    List<ChallengeResponseDto> selectInitialChallenges(List<ChallengeRequestDto> requestDtoList);
}

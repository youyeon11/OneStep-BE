package com.a508.onestep.domain.room.service;

import com.a508.onestep.domain.room.dto.request.TroubleCreateRequestDto;
import com.a508.onestep.domain.room.dto.response.SolutionResponseDto;

import java.util.List;

public interface TroubleService {
    Long createTrouble(TroubleCreateRequestDto requestDto);
    List<SolutionResponseDto> getAllSolution();
    SolutionResponseDto getSolution(Long solutionId);
}

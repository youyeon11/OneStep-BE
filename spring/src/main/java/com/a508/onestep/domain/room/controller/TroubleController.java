package com.a508.onestep.domain.room.controller;

import com.a508.onestep.domain.room.dto.request.TroubleCreateRequestDto;
import com.a508.onestep.domain.room.dto.response.SolutionResponseDto;
import com.a508.onestep.domain.room.dto.response.TroubleCreateResponseDto;
import com.a508.onestep.domain.room.service.TroubleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/troubles")
@RequiredArgsConstructor
@Tag(name = "Trouble Controller", description = "고민(trouble) 관련 API")
public class TroubleController {

    private final TroubleService troubleService;

    @PostMapping("")
    @Operation(summary = "내 고민 생성", description = "현재 로그인한 사용자의 고민을 생성합니다.")
    public TroubleCreateResponseDto createTrouble(
            @RequestBody TroubleCreateRequestDto requestDto
    ) {
        Long troubleId = troubleService.createTrouble(requestDto);

        return TroubleCreateResponseDto.builder()
                .troubleId(troubleId)
                .build();
    }

    @GetMapping("")
    @Operation(summary = "내 고민에 달린 답변들 전체 조회", description = "내 고민에 대해 달린 답변들을 전체 조회합니다")
    public List<SolutionResponseDto> getAll() {
        return troubleService.getAllSolution();
    }

    @GetMapping("/{solutionId}")
    @Operation(summary = "내 고민에 달린 답변 상세 조회",
            description = "내 고민에 대해 달린 특정 답변을 상세 조회합니다")
    public SolutionResponseDto getDetail(
            @PathVariable("solutionId") Long solutionId
    ) {
        return troubleService.getSolution(solutionId);
    }
}

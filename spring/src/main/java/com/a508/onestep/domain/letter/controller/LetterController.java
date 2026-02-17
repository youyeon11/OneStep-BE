package com.a508.onestep.domain.letter.controller;

import com.a508.onestep.domain.letter.dto.request.LetterCreateRequestDto;
import com.a508.onestep.domain.letter.dto.response.*;
import com.a508.onestep.domain.letter.service.LetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Letter", description = "편지 관련 API")
@RestController
@RequestMapping("/api/v1/letters")
@RequiredArgsConstructor
public class LetterController {

    private final LetterService letterService;

    /**
     * 1번 API: 편지 작성
     */
    @PostMapping("")
    @Operation(summary = "편지 작성", description = "새로운 편지를 작성하고 랜덤 수신자에게 배달을 예약합니다.")
    public Long createLetter(
            @Valid @RequestBody LetterCreateRequestDto requestDto
    ) {
        return letterService.createLetter(requestDto);
    }

    /**
     * 보관함 편지 목록 조회 (SAVED)
     */
    @GetMapping("")
    @Operation(summary = "보관함 편지 목록 조회", description = "보관한 편지 목록을 조회합니다.")
    public List<SavedLetterListItemResponseDto> getSavedLetters() {
        return letterService.getSavedLetters();
    }

    /**
     * 보관함 편지 상세 조회 (content만)
     */
    @GetMapping("/{letterId}")
    @Operation(summary = "보관함 편지 상세 조회", description = "보관한 편지의 내용을 조회합니다.")
    public SavedLetterDetailResponseDto getSavedLetterDetail(
            @PathVariable Long letterId
    ) {
        return letterService.getSavedLetterDetail(letterId);
    }

    /**
     * 편지 보관하기
     */
    @PatchMapping("/{letterId}/save")
    @Operation(summary = "편지 보관", description = "수신한 편지를 보관함에 저장합니다.")
    public LetterStatusResponseDto saveLetter(
            @PathVariable Long letterId
    ) {
        return letterService.saveLetter(letterId);
    }

    /**
     * 편지 삭제
     */
    @PatchMapping("/{letterId}/delete")
    @Operation(summary = "편지 삭제", description = "수신한 편지를 삭제합니다.")
    public LetterStatusResponseDto deleteLetter(
            @PathVariable Long letterId
    ) {
        return letterService.deleteLetter(letterId);
    }

    /**
     * 편지 수신 모드 현재 상태 조회 (isOpen 값 확인)
     */
    @GetMapping("/receive-status")
    @Operation(summary = "수신 모드 상태 조회", description = "현재 편지 수신 가능 상태(is_open)를 확인합니다.")
    public ReceiveStatusResponseDto getReceiveStatus() {
        return letterService.getReceiveStatus();
    }

    /**
     * 편지 수신 모드 온/오프 토글
     */
    @PatchMapping("/receive-status")
    @Operation(summary = "수신 모드 토글", description = "편지 수신 가능 상태(is_open)를 켜거나 끕니다.")
    public ReceiveStatusResponseDto toggleReceiveStatus() {
        return letterService.toggleReceiveStatus();
    }

    /**
     * 수신함 조회 (줍기만 하고 아직 보관/삭제를 안 한 목록)
     */
    @GetMapping("/inbox")
    @Operation(summary = "수신함 목록 조회",
            description = "새가 물고온 편지(UNSAVED) 목록을 조회합니다.")
    public LetterReceiveResponseDto getInboxLetters() {
        return letterService.receiveTodayLetter();
    }
}
package com.a508.onestep.domain.letter.service;

import com.a508.onestep.domain.letter.dto.request.LetterCreateRequestDto;
import com.a508.onestep.domain.letter.dto.response.*;

import java.util.List;

public interface LetterService {

    Long createLetter(LetterCreateRequestDto requestDto);

    LetterStatusResponseDto saveLetter(Long letterId);

    LetterStatusResponseDto deleteLetter(Long letterId);

    // 보관함 조회 (저장 처리를 진행한 목록)
    List<SavedLetterListItemResponseDto> getSavedLetters();

    ReceiveStatusResponseDto getReceiveStatus();

    SavedLetterDetailResponseDto getSavedLetterDetail(Long letterId);

    ReceiveStatusResponseDto toggleReceiveStatus();

    LetterReceiveResponseDto receiveTodayLetter();
}


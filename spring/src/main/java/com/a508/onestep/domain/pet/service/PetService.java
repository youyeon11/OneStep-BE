package com.a508.onestep.domain.pet.service;

import com.a508.onestep.domain.pet.dto.request.PetUpdateRequestDto;
import com.a508.onestep.domain.pet.dto.response.PetInfoResponseDto;

public interface PetService {
    PetInfoResponseDto getMyPet();

    PetInfoResponseDto updateMyPet(PetUpdateRequestDto requestDto);
}

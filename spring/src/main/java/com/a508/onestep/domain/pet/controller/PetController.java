package com.a508.onestep.domain.pet.controller;

import com.a508.onestep.domain.pet.dto.request.PetUpdateRequestDto;
import com.a508.onestep.domain.pet.dto.response.PetInfoResponseDto;
import com.a508.onestep.domain.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
@Tag(name = "Pet Controller", description = "펫 관련 API")
public class PetController {

    private final PetService petService;

    @GetMapping("")
    @Operation(summary = "나의 펫 조회", description = "현재 로그인한 사용자의 펫 정보를 조회합니다.")
    public PetInfoResponseDto getMyPet(
            @RequestHeader("Authorization") String token) {

        return petService.getMyPet();
    }

    @PatchMapping("")
    @Operation(summary = "나의 펫 정보 수정", description = "입력된 필드값만 수정합니다.")
    public PetInfoResponseDto updateMyPet(
            @RequestHeader("Authorization") String token,
            @RequestBody PetUpdateRequestDto requestDto
    ) {
        return petService.updateMyPet(requestDto);
    }
}

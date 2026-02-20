package com.a508.onestep.domain.pet.service;

import com.a508.onestep.domain.pet.dto.request.PetUpdateRequestDto;
import com.a508.onestep.domain.pet.dto.response.PetInfoResponseDto;
import com.a508.onestep.domain.pet.entity.PetOwnership;
import com.a508.onestep.domain.pet.repository.PetOwnershipRepository;
import com.a508.onestep.domain.pet.util.PetLevelCalculator;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.exception.ResourceNotFoundException;
import com.a508.onestep.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class PetServiceImpl implements PetService {

    private final UserRepository userRepository;
    private final PetOwnershipRepository petOwnershipRepository;

    @Override
    public PetInfoResponseDto getMyPet() {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        PetOwnership petOwnership = petOwnershipRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자의 펫을 찾을 수 없습니다."));

        int totalExp = user.getTotalExp() == null ? 0 : user.getTotalExp();
        int level = petOwnership.getPetLevel();
        int maxExp = petOwnership.getMaxExp();

        int levelStartExp = PetLevelCalculator.getLevelStartExp(level);
        int levelEndExp = (maxExp == -1) ? -1 : PetLevelCalculator.getLevelStartExp(level + 1);

        return PetInfoResponseDto.petInfoResponseDtoOf(
                petOwnership.getId(),
                petOwnership.getPetCode(),
                petOwnership.getPetNickname(),
                petOwnership.getCurrentExp(),
                maxExp,
                level,
                totalExp,
                levelStartExp,
                levelEndExp
        );
    }


    @Override
    @Transactional
    public PetInfoResponseDto updateMyPet(PetUpdateRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        PetOwnership petOwnership = petOwnershipRepository.findByUserId(user.getId())
                .orElseThrow(() -> BusinessException.of(ErrorCode.PET_NOT_FOUND));

        if (requestDto.getAddedExp() != null && requestDto.getAddedExp() > 0) {
            user.addTotalExp(requestDto.getAddedExp());

            int total = user.getTotalExp();
            int newLevel = PetLevelCalculator.getPetLevel(total);
            int currentExp = PetLevelCalculator.getCurrentExpInLevel(total, newLevel);
            int maxExp = PetLevelCalculator.getMaxExpForLevel(newLevel);

            petOwnership.updateExpInfo(newLevel, currentExp, maxExp);
        }

        // 펫 닉네임 변경
        if (requestDto.getPetNickname() != null) {
            petOwnership.updatePetNickname(requestDto.getPetNickname());
        }
        // 펫 코드 변경
        if (requestDto.getPetCode() != null) {
            petOwnership.updatePetCode(requestDto.getPetCode());
        }
        // 3) 최종 상태 기준으로 응답용 값 재계산
        int totalExp = user.getTotalExp() == null ? 0 : user.getTotalExp();
        int level = petOwnership.getPetLevel();
        int maxExp = petOwnership.getMaxExp();

        int levelStartExp = PetLevelCalculator.getLevelStartExp(level);
        int levelEndExp = (maxExp == -1) ? -1 : PetLevelCalculator.getLevelStartExp(level + 1);

        // 4) 응답 반환
        return PetInfoResponseDto.petInfoResponseDtoOf(
                petOwnership.getId(),
                petOwnership.getPetCode(),
                petOwnership.getPetNickname(),
                petOwnership.getCurrentExp(),
                maxExp,
                level,
                totalExp,
                levelStartExp,
                levelEndExp
        );
    }
}
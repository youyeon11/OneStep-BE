package com.a508.onestep.domain.user.service;

import com.a508.onestep.domain.pet.entity.PetOwnership;
import com.a508.onestep.domain.pet.repository.PetOwnershipRepository;
import com.a508.onestep.domain.user.dto.request.UserSurveyRequestDto;
import com.a508.onestep.domain.user.entity.SurveyLog;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.SurveyRepository;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.kafka.dto.UserInitialJoinSet;
import com.a508.onestep.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class UserTransactionHelper {

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final PetOwnershipRepository petOwnershipRepository;

    @Transactional
    public UserInitialJoinSet executeRegisterSurvey(UserSurveyRequestDto requestDto, String userCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        // DTO에서 계산된 총점을 엔티티에 반영 (엔티티가 알아서 상태 판정)
        user.updateSurveyResult(requestDto.getTotalScore());
        List<Integer> answerList = requestDto.getAnswers();

        // 15개 설문 같이 저장
        List<SurveyLog> surveyLogs = IntStream.range(0, answerList.size())
                .mapToObj(i -> new SurveyLog(user, i + 1, answerList.get(i)))
                .collect(Collectors.toList());

        PetOwnership pet = petOwnershipRepository.findByUserId(user.getId())
                .orElseThrow(() -> BusinessException.of(ErrorCode.PET_NOT_FOUND));
        String petNickname = pet.getPetNickname();
        UserInitialJoinSet joinSet = UserInitialJoinSet.builder()
                .userCode(userCode)
                .recoveryLevel(user.getRecoveryLevel())
                .petNickname(petNickname)
                .build();
        surveyRepository.saveAll(surveyLogs);

        return joinSet;
    }
}

package com.a508.onestep.domain.user.service;

import com.a508.onestep.domain.pet.entity.PetOwnership;
import com.a508.onestep.domain.pet.repository.PetOwnershipRepository;
import com.a508.onestep.domain.user.dto.request.UserAgreementRequestDto;
import com.a508.onestep.domain.user.dto.request.UserResurveyRequestDto;
import com.a508.onestep.domain.user.dto.request.UserSurveyRequestDto;
import com.a508.onestep.domain.user.dto.request.UserUpdateRequestDto;
import com.a508.onestep.domain.user.dto.response.UserInfoResponseDto;
import com.a508.onestep.domain.user.entity.SurveyLog;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.SurveyRepository;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.kafka.dto.KafkaMessageDto;
import com.a508.onestep.global.kafka.dto.UserInitialJoinSet;
import com.a508.onestep.global.kafka.producer.KafkaProducer;
import com.a508.onestep.global.kafka.topic.KafkaTopics;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final PetOwnershipRepository petOwnershipRepository;
    private final KafkaProducer kafkaProducer;

    @Override
    @Transactional
    public UserInfoResponseDto updateMyUserInfo(UserUpdateRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        if (requestDto.getNickname() != null) {
            if (requestDto.getNickname().isBlank()) {
                throw BusinessException.of(ErrorCode.INVALID_NICKNAME);
            }
            user.updateNickname(requestDto.getNickname());
        }

        return UserInfoResponseDto.from(user);
    }

    @Override
    public UserInfoResponseDto getMyUserInfo() {
        String userCode = UserContextHolder.getUserCode();
        LogUtils.info("회원 정보 조회: userCode={}", userCode);

        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        return UserInfoResponseDto.from(user);
    }

    @Override
    @Transactional
    public UserInfoResponseDto updateAgreements(UserAgreementRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        user.updateAgreements(requestDto.getIsLocationAllowed(), requestDto.getIsAlarmAllowed());

        return UserInfoResponseDto.from(user);
    }

    @Override
    @Transactional
    public void registerSurvey(UserSurveyRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
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

        // DB 저장 후 전달
        KafkaMessageDto<UserInitialJoinSet> message = KafkaMessageDto.<UserInitialJoinSet>builder()
                .id(UUID.randomUUID().toString())
                .type("INITIAL_SET")
                .payload(joinSet)
                .timestamp(System.currentTimeMillis())
                .build();
        kafkaProducer.send(KafkaTopics.INITIAL_USER_INFO, userCode, message);
    }

    @Override
    @Transactional
    public UserInfoResponseDto resurvey(UserResurveyRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        SurveyLog targetLog = surveyRepository.findBySurveyNumberAndUser(
                        requestDto.getSurveyNumber(),
                        user.getId())
                .orElseThrow(() -> BusinessException.of(ErrorCode.SURVEY_LOG_NOT_FOUND));

        LogUtils.debug("{} 번 설문 조사 업데이트", requestDto.getSurveyNumber());
        targetLog.updateAnswer(requestDto.getAnswer());
        List<SurveyLog> allSurveyLogs = surveyRepository.findByUserId(user.getId());
        int totalScore = allSurveyLogs.stream()
                .mapToInt(SurveyLog::getAnswer)
                .sum();
        user.updateSurveyResult(totalScore);

        return UserInfoResponseDto.from(user);
    }
}
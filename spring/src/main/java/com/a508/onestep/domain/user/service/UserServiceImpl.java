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
import com.a508.onestep.global.exception.SemaphoreAcquisitionException;
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
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final KafkaProducer kafkaProducer;
    private final Semaphore databaseSemaphore;

    private final UserTransactionHelper userTransactionHelper;

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
    public void registerSurvey(UserSurveyRequestDto requestDto) {
        UserInitialJoinSet joinSet;
        String userCode = UserContextHolder.getUserCode();

        boolean acquired = false;
        try {
            databaseSemaphore.acquire();
            acquired = true;
            joinSet = userTransactionHelper.executeRegisterSurvey(requestDto, userCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SemaphoreAcquisitionException.of("DB 접근 대기 중 인터럽트 발생", e);
        } finally {
            if (acquired) databaseSemaphore.release();
        }

        // Kafka send AFTER semaphore release and transaction
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
package com.a508.onestep.domain.challenge.service;

import com.a508.onestep.domain.challenge.dto.request.ChallengeCompleteRequestDto;
import com.a508.onestep.domain.challenge.dto.request.ChallengeRequestDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeCompleteResponseDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeResponseDto;
import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import com.a508.onestep.domain.challenge.entity.ChallengeMaster;
import com.a508.onestep.domain.challenge.entity.RecommendedRoutine;
import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.challenge.repository.ChallengeAssignmentRepository;
import com.a508.onestep.domain.challenge.repository.ChallengeMasterRepository;
import com.a508.onestep.domain.challenge.repository.RecommendedRoutineRepository;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.kafka.producer.KafkaProducer;
import com.a508.onestep.global.kafka.topic.KafkaTopics;
import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChallengeTransactionHelper {

    private final RecommendedRoutineRepository recommendedRoutineRepository;
    private final ChallengeMasterRepository challengeMasterRepository;
    private final ChallengeAssignmentRepository challengeAssignmentRepository;
    private final UserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaProducer kafkaProducer;

    @Transactional
    public ChallengeResponseDto register(String userCode, ChallengeRequestDto requestDto) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        ChallengeAssignment challengeAssignment = ChallengeAssignment.builder()
                .user(user)
                .challengeStatus(AssignmentStatus.ASSIGNED)
                .content(requestDto.getContent())
                .origin(Origin.SELF)
                .assignedDate(today)
                .exp(0)
                .assignedDate(today)
                .build();

        ChallengeAssignment savedChallenge = challengeAssignmentRepository.save(challengeAssignment);

        return ChallengeResponseDto.builder()
                .challengeId(savedChallenge.getId())
                .content(savedChallenge.getContent())
                .origin(savedChallenge.getOrigin().name())
                .challengeStatus(savedChallenge.getChallengeStatus().name())
                .exp(0)
                .build();
    }

    @Transactional
    public ChallengeCompleteResponseDto complete(String userCode, ChallengeCompleteRequestDto requestDto) {
        ChallengeAssignment challengeAssignment = challengeAssignmentRepository
                .findByIdWithUser(requestDto.getChallengeId())
                .orElseThrow(() -> BusinessException.of(ErrorCode.CHALLENGE_NOT_FOUND));

        if (challengeAssignment.getChallengeStatus().equals(AssignmentStatus.COMPLETED)) {
            throw BusinessException.of(ErrorCode.CHALLENGE_ALREADY_DONE);
        }
        if (!challengeAssignment.getUser().getUserCode().equals(userCode)) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }

        challengeAssignment.complete(requestDto.getEmotion());

        ChallengeCompletedEvent event = ChallengeCompletedEvent.fromChallengeAssignment(challengeAssignment);
        eventPublisher.publishEvent(event);

        return ChallengeCompleteResponseDto.from(challengeAssignment);
    }

    @Transactional
    public List<ChallengeResponseDto> selectInitialChallenges(String userCode, List<ChallengeRequestDto> requestDto) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        List<ChallengeAssignment> challengeAssignmentList = requestDto.stream()
                .map(dto -> ChallengeAssignment.builder()
                        .challengeMasterId(dto.getMasterChallengeId() == null ? null
                                : dto.getMasterChallengeId())
                        .user(user)
                        .content(dto.getContent())
                        .exp(dto.getExp())
                        .challengeStatus(AssignmentStatus.ASSIGNED)
                        .origin(Origin.RECOMMENDED)
                        .assignedDate(today)
                        .build())
                .toList();

        challengeAssignmentRepository.saveAll(challengeAssignmentList);

        return challengeAssignmentList.stream()
                .map(ChallengeResponseDto::from)
                .toList();
    }

    public List<ChallengeResponseDto> getChallenge(String userCode) {
        LogUtils.info("유저 코드 받아 오기, payload={}", userCode);

        // Step 1: Check existing assignments (quick DB read)
        List<ChallengeResponseDto> existing = checkExistingAssignments(userCode);
        if (existing != null) {
            return existing;
        }

        // Step 2: MongoDB read OUTSIDE transaction (no tx needed)
        RecommendedRoutine recommendedRoutine = recommendedRoutineRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.RECOMMENDATION_NOT_FOUND));

        LogUtils.info("1. 몽고DB에서 데이터를 가져온다, payload={}", recommendedRoutine);

        List<RecommendedRoutine.Recommendation> recommendations = recommendedRoutine.getRecommendations();
        LogUtils.info("몽고DB에서 가져온 챌린지 리스트 생성, payload={}", recommendations);

        List<Long> challengeIdList = recommendations.stream()
                .map(RecommendedRoutine.Recommendation::getChallengeCode)
                .toList();

        // Step 3: DB operations in transaction
        List<ChallengeResponseDto> result = saveChallengeAssignments(userCode, challengeIdList);

        // Step 4: Kafka send AFTER transaction
        kafkaProducer.send(KafkaTopics.VISITED_USER, userCode);

        return result;
    }

    @Transactional(readOnly = true)
    protected List<ChallengeResponseDto> checkExistingAssignments(String userCode) {
        List<ChallengeAssignment> existingChllengeAssignmentList = challengeAssignmentRepository
                .findByUserCodeAndAssignedDateAndOrigin(userCode, LocalDate.now(), Origin.RECOMMENDED);
        if (existingChllengeAssignmentList != null && !existingChllengeAssignmentList.isEmpty()) {
            return existingChllengeAssignmentList.stream()
                    .map(ChallengeResponseDto::from)
                    .toList();
        }
        return null;
    }

    @Transactional
    protected List<ChallengeResponseDto> saveChallengeAssignments(String userCode, List<Long> challengeIdList) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        LogUtils.info("2. ChallengeMaster에 존재하는지 여부 확인, payload={}", challengeIdList);

        List<ChallengeMaster> foundMasters = challengeMasterRepository.findAllById(challengeIdList);

        LogUtils.info("챌린지 마스터 객체 리스트 생성, payload={}", foundMasters);

        if (foundMasters.size() != challengeIdList.size()) {
            Set<Long> foundIds = foundMasters.stream()
                    .map(ChallengeMaster::getId)
                    .collect(Collectors.toSet());
            List<Long> missingIds = challengeIdList.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            LogUtils.error("존재하지 않는 Id : " + missingIds);
            throw BusinessException.of(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        Map<Long, ChallengeMaster> challengeMasterMap = foundMasters.stream()
                .collect(Collectors.toMap(
                        ChallengeMaster::getId,
                        master -> master
                ));

        List<ChallengeAssignment> challengeAssignmentList = challengeIdList.stream()
                .map(challengeId -> {
                    ChallengeMaster master = challengeMasterMap.get(challengeId);

                    return ChallengeAssignment.builder()
                            .assignedDate(LocalDate.now())
                            .challengeStatus(AssignmentStatus.ASSIGNED)
                            .content(master.getTitle())
                            .origin(Origin.RECOMMENDED)
                            .exp(master.getReward())
                            .user(user)
                            .challengeMasterId(master.getId())
                            .build();
                })
                .toList();

        challengeAssignmentRepository.saveAll(challengeAssignmentList);
        LogUtils.info("{}개의 챌린지가 정상 저장되었습니다.", challengeAssignmentList.size());

        return challengeAssignmentList.stream()
                .map(ChallengeResponseDto::from)
                .toList();
    }
}
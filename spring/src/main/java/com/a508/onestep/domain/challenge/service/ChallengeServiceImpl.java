package com.a508.onestep.domain.challenge.service;

import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import com.a508.onestep.domain.challenge.entity.ChallengeMaster;
import com.a508.onestep.domain.challenge.entity.RecommendedRoutine;
import com.a508.onestep.domain.challenge.repository.ChallengeAssignmentRepository;
import com.a508.onestep.domain.challenge.repository.ChallengeMasterRepository;
import com.a508.onestep.domain.challenge.repository.RecommendedRoutineRepository;
import com.a508.onestep.domain.challenge.dto.request.ChallengeCompleteRequestDto;
import com.a508.onestep.domain.challenge.dto.request.ChallengeRequestDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeCompleteResponseDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeResponseDto;
import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.challenge.dto.response.InitialChallengeResponseDto;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.common.TagCategory;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.kafka.producer.KafkaProducer;
import com.a508.onestep.global.kafka.topic.KafkaTopics;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final RecommendedRoutineRepository recommendedRoutineRepository;
    private final ChallengeMasterRepository challengeMasterRepository;
    private final ChallengeAssignmentRepository challengeAssignmentRepository;
    private final UserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaProducer kafkaProducer;

    /*
     * 챌린지(할 일) 등록하기
     */
    @Override
    @Transactional
    public ChallengeResponseDto register(ChallengeRequestDto requestDto) {

        // user 확인
        String userCode = UserContextHolder.getUserCode();
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

    /*
     * 내가 추가한 챌린지(할 일) 조회하기
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponseDto> getAll() {
        String userCode = UserContextHolder.getUserCode();

        LocalDate today = LocalDate.now();
        List<ChallengeAssignment> challengeAssignmentList = challengeAssignmentRepository
                .findByUserCodeAndAssignedDateAndOrigin(userCode, today, Origin.SELF);
        return challengeAssignmentList.stream()
                .map(ChallengeResponseDto::from)
                .toList();
    }

    /*
     * 챌린지 완료 체크하기
     */
    @Override
    @Transactional
    public ChallengeCompleteResponseDto complete(ChallengeCompleteRequestDto requestDto) {

        // 사용자 찾기
        String userCode = UserContextHolder.getUserCode();

        ChallengeAssignment challengeAssignment = challengeAssignmentRepository
                .findByIdWithUser(requestDto.getChallengeId())
                .orElseThrow(() -> BusinessException.of(ErrorCode.CHALLENGE_NOT_FOUND));

        // 이미 완료된 거면 안 됨
        if (challengeAssignment.getChallengeStatus().equals(AssignmentStatus.COMPLETED)) {
            throw BusinessException.of(ErrorCode.CHALLENGE_ALREADY_DONE);
        }
        // 해당 사용자의 Challenge 맞는지 조회 (JOIN FETCH로 가져온 user 활용)
        if (!challengeAssignment.getUser().getUserCode().equals(userCode)) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }

        challengeAssignment.complete(requestDto.getEmotion());

        // 완료 후 Event 발행
        ChallengeCompletedEvent event = ChallengeCompletedEvent.fromChallengeAssignment(challengeAssignment);
        eventPublisher.publishEvent(event);

        return ChallengeCompleteResponseDto.from(challengeAssignment);
    }

    /*
     * 랜덤으로 20개 선정
     */
    @Override
    public List<InitialChallengeResponseDto> getInitialRecommendations() {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        Integer recoveryLevel = user.getRecoveryLevel();
        List<TagCategory> categories = List.of(TagCategory.values());
        List<ChallengeMaster> challengeMasterList = challengeMasterRepository
                .findRecommendedChallenges(recoveryLevel, categories, user);

        return challengeMasterList.stream()
                .map(InitialChallengeResponseDto::from)
                .toList();
    }

    /*
     * 여러개를 선택해서 저장
     */
    @Transactional
    @Override
    public List<ChallengeResponseDto> selectInitialChallenges(List<ChallengeRequestDto> requestDto) {

        // user 확인
        String userCode = UserContextHolder.getUserCode();
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

    @Transactional
    @Override
    public List<ChallengeResponseDto> getChallenge() {
        /**
         * 첫 조회 시 일일 추천 챌린지 (5개) 반환
         * @return List<RecommendedRoutineDto>
         */

        // 유저 코드 받아 오기
        String userCode = UserContextHolder.getUserCode();

        LogUtils.info("유저 코드 받아 오기, payload={}", userCode);
        List<ChallengeAssignment> existingChllengeAssignmentList = challengeAssignmentRepository
                .findByUserCodeAndAssignedDateAndOrigin(userCode, LocalDate.now(), Origin.RECOMMENDED);
        if (existingChllengeAssignmentList != null && !existingChllengeAssignmentList.isEmpty()) {
            return existingChllengeAssignmentList.stream()
                    .map(ChallengeResponseDto::from)
                    .toList();
        }

        // 새 할당 생성이 필요한 경우에만 user 조회
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        // MongoDB로부터 추천 가져오기
        RecommendedRoutine recommendedRoutine = recommendedRoutineRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.RECOMMENDATION_NOT_FOUND));

        LogUtils.info("MongoDB로부터 가져온 RecommendedRoutine={}", recommendedRoutine);

        List<RecommendedRoutine.Recommendation> recommendations = recommendedRoutine.getRecommendations();

        // challengeMasterId 리스트 생성
        List<Long> challengeIdList = recommendations.stream()
                .map(RecommendedRoutine.Recommendation::getChallengeCode)
                .toList();
        List<ChallengeMaster> foundMasters = challengeMasterRepository.findAllById(challengeIdList);

        LogUtils.info("챌린지 마스터 객체 리스트 생성, payload={}", foundMasters);

        if (foundMasters.size() != challengeIdList.size()) {
            // 존재하는 번호 집합 만들기
            Set<Long> foundIds = foundMasters.stream()
                    .map(ChallengeMaster::getId)
                    .collect(Collectors.toSet());
            // 없는 번호 리스트 생성
            List<Long> missingIds = challengeIdList.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            // 없는 챌린지 반환 및 에러 전달
            LogUtils.error("존재하지 않는 Id : " + missingIds);
            throw BusinessException.of(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        // ChallengeMaster ID Mapping
        Map<Long, ChallengeMaster> challengeMasterMap = foundMasters.stream()
                .collect(Collectors.toMap(
                        ChallengeMaster::getId, // challengeMaster의 id를 key로
                        master -> master // challengeMaster를 value로
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

        List<ChallengeResponseDto> recommendedRoutineList = challengeAssignmentList.stream()
                .map(ChallengeResponseDto::from)
                .toList();

        // UserCode 저장
        kafkaProducer.send(KafkaTopics.VISITED_USER, userCode);
        return recommendedRoutineList;
    }
}
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
    챌린지(할 일) 등록하기
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
    내가 추가한 챌린지(할 일) 조회하기
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponseDto> getAll() {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        List<ChallengeAssignment> challengeAssignmentList = challengeAssignmentRepository.findSelfByUserIdAndAssignedDate(user.getId(), today, Origin.SELF);
        return challengeAssignmentList.stream()
                .map(ChallengeResponseDto::from)
                .toList();
    }

    /*
    챌린지 완료 체크하기
     */
    @Override
    @Transactional
    public ChallengeCompleteResponseDto complete(ChallengeCompleteRequestDto requestDto) {

        // 사용자 찾기
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                        .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        ChallengeAssignment challengeAssignment = challengeAssignmentRepository.findById(requestDto.getChallengeId())
                .orElseThrow(() -> BusinessException.of(ErrorCode.CHALLENGE_NOT_FOUND));

        // 이미 완료된 거면 안 됨
        if (challengeAssignment.getChallengeStatus().equals(AssignmentStatus.COMPLETED)) {
            throw BusinessException.of(ErrorCode.CHALLENGE_ALREADY_DONE);
        }
        // 해당 사용자의 Challenge 맞는지 조회
        if (challengeAssignment.getUser().getId() != user.getId()) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }

        challengeAssignment.complete(requestDto.getEmotion());

        // 완료 후 Event 발행
        ChallengeCompletedEvent event = ChallengeCompletedEvent.fromChallengeAssignment(challengeAssignment);
        eventPublisher.publishEvent(event);

        return ChallengeCompleteResponseDto.from(challengeAssignment);
    }

    /*
    랜덤으로 20개 선정
     */
    @Override
    public List<InitialChallengeResponseDto> getInitialRecommendations() {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        Integer recoveryLevel = user.getRecoveryLevel();
        List<TagCategory> categories = List.of(TagCategory.values());
        List<ChallengeMaster> challengeMasterList =
                challengeMasterRepository.findRecommendedChallenges(recoveryLevel, categories, user);

        return challengeMasterList.stream()
                .limit(20)
                .map(InitialChallengeResponseDto::from)
                .toList();
    }

    /*
    여러개를 선택해서 저장
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
                        .challengeMasterId(dto.getMasterChallengeId() == null ? null : dto.getMasterChallengeId())
                        .user(user)
                        .content(dto.getContent())
                        .exp(dto.getExp())
                        .challengeStatus(AssignmentStatus.ASSIGNED)
                        .origin(Origin.RECOMMENDED)
                        .assignedDate(today)
                        .build()
                )
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
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        Long userId = user.getId();

        LogUtils.info("유저 코드 받아 오기, payload={}",user);
        List<ChallengeAssignment> existingChllengeAssignmentList = challengeAssignmentRepository.findSelfByUserIdAndAssignedDate(userId, LocalDate.now(), Origin.RECOMMENDED);
        if (existingChllengeAssignmentList != null && !existingChllengeAssignmentList.isEmpty()) {
            return existingChllengeAssignmentList.stream()
                    .map(ChallengeResponseDto::from)
                    .toList();
        }

        // 1. 몽고DB에서 데이터를 가져온다
        RecommendedRoutine recommendedRoutine = recommendedRoutineRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.RECOMMENDATION_NOT_FOUND));
        
        LogUtils.info("1. 몽고DB에서 데이터를 가져온다, payload={}",recommendedRoutine);

        // 몽고DB에서 가져온 챌린지 리스트 생성
        List<RecommendedRoutine.Recommendation> recommendations = recommendedRoutine.getRecommendations();
        
        LogUtils.info("몽고DB에서 가져온 챌린지 리스트 생성, payload={}",recommendations);

        // 2. ChallengeMaster에 존재하는지 여부 확인
        // challengeId 리스트 생성
        List<Long> challengeIdList = recommendations.stream()
                .map(RecommendedRoutine.Recommendation::getChallengeCode)
                .toList();
        LogUtils.info("2. ChallengeMaster에 존재하는지 여부 확인, payload={}",challengeIdList);

        // 챌린지 마스터 객체 리스트 생성
        List<ChallengeMaster> foundMasters = challengeMasterRepository.findAllById(challengeIdList);

        LogUtils.info("챌린지 마스터 객체 리스트 생성, payload={}",foundMasters);

        // ChallengeMaster에 존재하는지 여부 확인
        // 누락된 챌린지 번호 확인 및 에러 반환
        if (foundMasters.size() != challengeIdList.size()) {
            //존재하는 번호 집합 만들기
            Set<Long> foundIds = foundMasters.stream()
                    .map(ChallengeMaster::getId)
                    .collect(Collectors.toSet());
            //없는 번호 리스트 생성
            List<Long> missingIds = challengeIdList.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            // 없는 챌린지 반환 및 에러 전달
            LogUtils.error("존재하지 않는 Id : " + missingIds);
            throw BusinessException.of(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        // 3. challengeId를 key로 하는 Map 생성
        Map<Long, ChallengeMaster> challengeMasterMap = foundMasters.stream()
                .collect(Collectors.toMap(
                        ChallengeMaster::getId, // challengeMaster의 id를 key로
                        master -> master // challengeMaster를 value로
                ));

        //3. 포스트그리에 저장
        // 저장할 챌린지 리스트 작성(ChallengeAssignment)
        List<ChallengeAssignment> challengeAssignmentList = challengeIdList.stream()
                .map(challengeId -> {
                    // map에서 challengeId로 객체를 꺼낸다
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
        // 한번에 저장
        challengeAssignmentRepository.saveAll(challengeAssignmentList);
        LogUtils.info("{}개의 챌린지가 정상 저장되었습니다.", challengeAssignmentList.size());

        // DTO 객체 리스트 생성
        List<ChallengeResponseDto> recommendedRoutineList = challengeAssignmentList.stream()
                .map(ChallengeResponseDto::from)
                .toList();

        // 4. 카프카에 유저 코드 저장
        kafkaProducer.send(KafkaTopics.VISITED_USER, userCode);
        return recommendedRoutineList;
    }

}

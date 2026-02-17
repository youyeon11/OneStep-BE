package com.a508.onestep.domain.challenge.service;

import com.a508.onestep.domain.challenge.entity.ChallengeAssignment;
import com.a508.onestep.domain.challenge.repository.ChallengeAssignmentRepository;
import com.a508.onestep.domain.challenge.dto.response.DailyDetailResponseDto;
import com.a508.onestep.domain.challenge.dto.response.DailyEmotionResponseDto;
import com.a508.onestep.domain.common.AssignmentStatus;
import com.a508.onestep.domain.route.entity.RouteSession;
import com.a508.onestep.domain.route.repository.RouteSessionRepository;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final ChallengeAssignmentRepository challengeAssignmentRepository;
    private final RouteSessionRepository routeSessionRepository;
    private final UserRepository userRepository;

    @Override
    public List<DailyEmotionResponseDto> getMonthlyEmotions(int year, int month) {
        /**
         * 1개월 기간동안 챌린지 수행 및 감정 기록 조회
         * @param year 조회할 연도
         * @param month 조회할 월
         * @return 날짜별로 일자와 평균 감정 기록을 리스트로 반환([ {일자 : (날짜), 감정 : (평균)} )
         */
        // 조회 시작일, 조회 마지막일 변수 저장
        if (month > 12 || month < 1) {
            throw BusinessException.of(ErrorCode.INVALID_DATE_RANGE);
        }
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // 유저 코드
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        // 챌린지 완료 여부
        AssignmentStatus status = AssignmentStatus.COMPLETED;
        // 완료한 챌린지 리스트
        List<ChallengeAssignment> challengeList = new ArrayList<>();

        try {
            challengeList = challengeAssignmentRepository.findByUserAndLogDateBetweenAndCompleted(user.getId(), startDate, endDate, status);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.CALENDAR_RETRIEVAL_FAIL);
        }

        Map<LocalDate, List<ChallengeAssignment>> DateGroup = challengeList.stream()
                .collect(Collectors.groupingBy(ChallengeAssignment::getLogDate));

        List<DailyEmotionResponseDto> responseDtoList = DateGroup.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<ChallengeAssignment> dayList = entry.getValue();

                    Double avg = dayList.stream()
                            .map(ChallengeAssignment::getEmotion) // 1~5
                            .filter(Objects::nonNull)
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0.0);

                    return new DailyEmotionResponseDto(date, Math.round(avg));
                })
                .sorted(Comparator.comparing(DailyEmotionResponseDto::getDate))
                .collect(Collectors.toList());

        return responseDtoList;
    }


    @Override
    public DailyDetailResponseDto getDetail(String date) {
        /**
         * 현재 로그인한 사용자의 특정 날짜에 완료한 챌린지 상세를 조회하는 메서드
         * @param date 조회할 날짜
         * @return ApiResponse.success타입으로 반환. 수행한 챌린지 ID와 내용(API 명세 상 content, DB에는 title이라고 저장되어 있음)을 담고 있는 객체 리스트인 completedChallenges와 감정 평균(average), walkImageUrl 반환
         */

        // 유저 코드 받기
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        // -------챌린지-------
        // 챌린지 완료 여부
        AssignmentStatus status = AssignmentStatus.COMPLETED;

        // 챌린지 정보 받아오기
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw BusinessException.of(ErrorCode.INVALID_DATE_FORMAT);
        }

        List<ChallengeAssignment> ChallengeList = new ArrayList<>();
        try {
            ChallengeList = challengeAssignmentRepository.findByUserAndLogDateAndCompleted(user.getId(), localDate, status);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        // -------산책-------
        RouteSession routesession = routeSessionRepository.findByUserIdAndLogDate(user.getId(), localDate)
                .orElse(routesession = null);

        DailyDetailResponseDto responseDto;
        try {
            responseDto = mapToDailyDetailResponseDto(ChallengeList, routesession);
        } catch (Exception e){
            throw BusinessException.of(ErrorCode.CANNOT_CONVERT_TO_RESPONSE);
        }

        return responseDto;
    }

    private DailyDetailResponseDto mapToDailyDetailResponseDto(List<ChallengeAssignment> challengeAssignments, RouteSession routesession) {
        /**
         * ChallengeAssignment 리스트를 받아서 Dto로 변환하는 함수
         * @param List<ChallengeAssignment> 그날 수행한 챌린지 데이터 리스트
         * @return DailyDetailResponseDto 만들어서 반환
         */

        List<DailyDetailResponseDto.ChallengeAssignmentDto> challengeAssignmentDtoList = new ArrayList<>();
        Long sum = 0L;

        String walkImageUrl = null;
        // 산책 있는 경우
        if (routesession != null && routesession.getImageUrl() != null) {
            walkImageUrl = routesession.getImageUrl();
        }

        // 평균 계산
        for (ChallengeAssignment assignment : challengeAssignments) {
            challengeAssignmentDtoList.add(DailyDetailResponseDto
                    .ChallengeAssignmentDto
                    .builder()
                    .challengeId(assignment.getId())
                    .content(assignment.getContent())
                    .build());
            sum += assignment.getEmotion();
        }

        Long average = challengeAssignments.isEmpty() ? 0L : Math.round((double)sum / challengeAssignments.size());

        return DailyDetailResponseDto.builder()
                .completedChallenges(challengeAssignmentDtoList)
                .average(average)
                .walkImageUrl(walkImageUrl)
                .build();
    }
}

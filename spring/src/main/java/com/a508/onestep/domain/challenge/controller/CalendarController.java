package com.a508.onestep.domain.challenge.controller;

import com.a508.onestep.domain.challenge.dto.response.DailyDetailResponseDto;
import com.a508.onestep.domain.challenge.dto.response.DailyEmotionResponseDto;
import com.a508.onestep.domain.challenge.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendars")
@RequiredArgsConstructor
@Tag(name = "Calendar Controller", description = "캘린더 관련 API")
public class CalendarController {
    private final CalendarService calendarService;

    @GetMapping("")
    @Operation(summary = "감정 월별 조회", description = "현재 로그인한 사용자의 월간 감정 정보를 조회합니다.")
    public List<DailyEmotionResponseDto> getDailyEmotion(
            @RequestHeader("Authorization") String token,
            @RequestParam int year,
            @RequestParam int month
    ) {
        /**
         * 현재 로그인한 사용자의 월간 감정 정보를 조회하는 메서드
         * @param year 조회할 연도
         * @param month 조회할 월
         * @return [날짜, 일별 감정 평균값] 리스트 반환
         */
        return calendarService.getMonthlyEmotions(year, month);
    }

    @GetMapping("/detail")
    @Operation(summary = "날짜별 완료한 챌린지 상세", description = "현재 로그인한 사용자의 날짜별 완료한 챌린지 상세 정보를 조회합니다.")
    public DailyDetailResponseDto getDailyDetail(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date
    ) {
        /**
         * 현재 로그인한 사용자의 특정 날짜에 완료한 챌린지 상세를 조회하는 메서드
         * @param date 조회할 날짜
         * @return ApiResponse.success타입으로 반환. 수행한 챌린지 ID와 내용(API 명세 상 content, DB에는 title이라고 저장되어 있음)을 담고 있는 객체 리스트인 completedChallenges와 감정 평균(average), walkImageUrl 반환
         */
        return calendarService.getDetail(date);
    }
}

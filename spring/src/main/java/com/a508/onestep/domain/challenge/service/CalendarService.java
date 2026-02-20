package com.a508.onestep.domain.challenge.service;

import com.a508.onestep.domain.challenge.dto.response.DailyDetailResponseDto;
import com.a508.onestep.domain.challenge.dto.response.DailyEmotionResponseDto;

import java.util.List;


public interface CalendarService {

    public List<DailyEmotionResponseDto> getMonthlyEmotions(int year, int month);

    public DailyDetailResponseDto getDetail(String date);
}

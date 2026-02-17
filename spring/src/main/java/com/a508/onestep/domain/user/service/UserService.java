package com.a508.onestep.domain.user.service;

import com.a508.onestep.domain.user.dto.request.UserAgreementRequestDto;
import com.a508.onestep.domain.user.dto.request.UserResurveyRequestDto;
import com.a508.onestep.domain.user.dto.request.UserSurveyRequestDto;
import com.a508.onestep.domain.user.dto.request.UserUpdateRequestDto;
import com.a508.onestep.domain.user.dto.response.UserInfoResponseDto;

public interface UserService {
    UserInfoResponseDto updateMyUserInfo(UserUpdateRequestDto requestDto);
    UserInfoResponseDto getMyUserInfo();
    UserInfoResponseDto updateAgreements(UserAgreementRequestDto requestDto);
    void registerSurvey(UserSurveyRequestDto requestDto);
    UserInfoResponseDto resurvey(UserResurveyRequestDto requestDto);
}
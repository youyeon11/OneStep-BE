package com.a508.onestep.global.auth.service;


import com.a508.onestep.global.auth.dto.request.KakaoLoginRequestDto;
import com.a508.onestep.global.auth.dto.request.LoginRequestDto;
import com.a508.onestep.global.auth.dto.response.LoginResponseDto;

public interface AuthService {

    LoginResponseDto kakaoLogin(KakaoLoginRequestDto requestDto);

    LoginResponseDto guestLogin(LoginRequestDto requestDto);

    void logout();

    LoginResponseDto reissue(String refreshToken);

    void linkToKakao(KakaoLoginRequestDto requestDto);

    void inactive();
}

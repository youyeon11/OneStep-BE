package com.a508.onestep.global.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KakaoLoginRequestDto {
    private String kakaoToken;
}

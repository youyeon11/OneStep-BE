package com.a508.onestep.global.auth.controller;

import com.a508.onestep.global.auth.dto.request.KakaoLoginRequestDto;
import com.a508.onestep.global.auth.dto.request.LoginRequestDto;
import com.a508.onestep.global.auth.dto.response.LoginResponseDto;
import com.a508.onestep.global.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = "인증 관련 API")
public class AuthController {

        private final AuthService authService;

        @PostMapping("/kakao")
        @Operation(summary = "카카오 로그인", description = """
                        카카오 OAuth 토큰으로 로그인하거나 회원가입을 진행합니다. <br>
                        안드로이드 앱에서 카카오 SDK로 획득한 카카오 Token을 전달하면
                        서버에서 사용자 정보를 조회하여 JWT 토큰을 발급합니다.
                        """)
        @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDto.class), examples = @ExampleObject(value = """
                        {
                            "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
                            "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
                            "userCode": "USR_12345678",
                            "nickname": "홍길동",
                            "isNew": true
                        }
                        """)))
        public LoginResponseDto kakaoLogin(
                        @RequestBody KakaoLoginRequestDto requestDto) {
                return authService.kakaoLogin(requestDto);
        }

        @PostMapping("/guest")
        @Operation(summary = "게스트 로그인", description = "닉네임만 입력하여 임시 계정으로 앱을 사용할 수 있습니다. 이미 로그인된 상태라면 기존 토큰을 재발급합니다.")
        @ApiResponse(responseCode = "200", description = "게스트 로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDto.class), examples = @ExampleObject(value = """
                        {
                            "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
                            "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
                            "userCode": "USR_87654321",
                            "isNew": true
                        }
                        """)))
        public LoginResponseDto guestLogin(
                        @RequestBody LoginRequestDto requestDto) {
                return authService.guestLogin(requestDto);
        }

        @PostMapping("/logout")
        @Operation(summary = "로그아웃", description = "Authorization 헤더의 JWT 토큰에서 사용자 정보를 추출해 로그아웃을 진행합니다.")
        public void logout(
                        @RequestHeader("Authorization") String token) {
                authService.logout();
        }

        @PostMapping("/reissue")
        @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. Refresh Token은 Redis에 저장된 값과 일치해야 합니다.")
        @ApiResponse(responseCode = "200", description = "토큰 재발급 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDto.class), examples = @ExampleObject(value = """
                        {
                            "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
                            "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
                            "userCode": "USR_12345678",
                            "isNew": false
                        }
                        """)))
        public LoginResponseDto reissue(
                        @RequestHeader("Authorization") String token) {
                if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                }
                return authService.reissue(token);
        }

        @PostMapping("/kakao/link")
        @Operation(summary = "카카오톡 연동하기", description = """
                기존의 회원이 카카오톡 연동을 할 수 있도록 이메일을 등록합니다.
                """)
        public void link(
                @RequestHeader("Authorization") String token,
                @RequestBody KakaoLoginRequestDto requestDto) {
                authService.linkToKakao(requestDto);
        }

        @PostMapping("/inactive")
        @Operation(summary = "회원 탈퇴하기", description = "회원 탈퇴 기능입니다.")
        public void incative(
                @RequestHeader("Authorization") String token
        ) {
                authService.inactive();
        }
}

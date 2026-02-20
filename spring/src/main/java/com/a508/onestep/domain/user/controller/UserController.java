package com.a508.onestep.domain.user.controller;

import com.a508.onestep.domain.user.dto.request.UserAgreementRequestDto;
import com.a508.onestep.domain.user.dto.request.UserResurveyRequestDto;
import com.a508.onestep.domain.user.dto.request.UserSurveyRequestDto;
import com.a508.onestep.domain.user.dto.request.UserUpdateRequestDto;
import com.a508.onestep.domain.user.dto.response.UserInfoResponseDto;
import com.a508.onestep.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "회원 관련 API")
public class UserController {

    private final UserService userService;

    @GetMapping("")
    @Operation(summary = "회원 정보 조회", description = "로그인된 사용자의 기본 회원 정보를 조회합니다.")
    public UserInfoResponseDto getMyUserInfo(
            @RequestHeader("Authorization") String token
    ) {
        return userService.getMyUserInfo();
    }

    @PatchMapping("")
    @Operation(summary = "회원 정보 수정", description = "로그인된 사용자의 회원 정보를 수정합니다.")
    public UserInfoResponseDto updateMyUserInfo(
            @RequestHeader("Authorization") String token,
            @RequestBody UserUpdateRequestDto requestDto
    ) {
        return userService.updateMyUserInfo(requestDto);
    }

    @PatchMapping("/agreement")
    @Operation(summary = "회원 동의 내용 수정", description = "로그인된 사용자의 서비스 이용 동의 설정을 수정합니다.")
    public UserInfoResponseDto updateAgreements(
            @RequestHeader("Authorization") String token,
            @RequestBody UserAgreementRequestDto requestDto
    ) {
        return userService.updateAgreements(requestDto);
    }

    @PostMapping("/survey")
    @Operation(summary = "설문 결과 등록", description = "사용자의 설문 응답을 제출하고 정보를 분석합니다.")
    public void registerSurvey(
            @RequestHeader("Authorization") String token,
            @RequestBody UserSurveyRequestDto requestDto
    ) {
        userService.registerSurvey(requestDto);
    }

    @PostMapping("/resurvey")
    @Operation(summary = "재설문하기", description = """
            사용자에게 질문을 다시 던질 때 사용합니다.<br>
            사용자가 답변을 달았을 때, 질문의 번호와 답변을 함께 작성해서 재설문을 등록하는 요청을 보내주세요.
            """)
    public UserInfoResponseDto resurvey(@RequestBody UserResurveyRequestDto requestDto) {
        return userService.resurvey(requestDto);
    }
}
package com.a508.onestep.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원 동의 내용 수정 요청 DTO")
public class UserAgreementRequestDto {

    @Schema(description = "위치 정보 제공 동의 여부", example = "true")
    private Boolean isLocationAllowed;

    @Schema(description = "알림 수신 동의 여부", example = "true")
    private Boolean isAlarmAllowed;
}
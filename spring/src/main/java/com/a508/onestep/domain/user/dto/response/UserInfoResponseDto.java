package com.a508.onestep.domain.user.dto.response;

import com.a508.onestep.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원 정보 조회 응답 DTO")
public class UserInfoResponseDto {

    @Schema(description = "사용자 닉네임", example = "손석우")
    private String nickname;

    @Schema(description = "서비스 이용 기간(일)", example = "30")
    private Integer daysTogether;

    @Schema(description = "위치 정보 제공 동의 여부", example = "true")
    private Boolean isLocationAllowed;

    @Schema(description = "알림 수신 동의 여부", example = "true")
    private Boolean isAlarmAllowed;

    public static UserInfoResponseDto from(User user) {

        int daysTogether = 0;
        if (user.getCreatedAt() != null) {
            daysTogether = (int) ChronoUnit.DAYS.between(
                    user.getCreatedAt(),
                    LocalDateTime.now()
            );
        }

        return UserInfoResponseDto.builder()
                .nickname(user.getNickname())
                .daysTogether(daysTogether)
                .isLocationAllowed(Boolean.TRUE.equals(user.getGpsOptIn()))
                .isAlarmAllowed(Boolean.TRUE.equals(user.getNotifOptIn()))
                .build();
    }

}


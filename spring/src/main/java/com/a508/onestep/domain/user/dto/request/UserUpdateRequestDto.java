package com.a508.onestep.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원 정보 수정 요청 DTO")
public class UserUpdateRequestDto {

    @Schema(description = "변경할 닉네임", example = "새닉네임")
    private String nickname;
}

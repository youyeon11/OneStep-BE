package com.a508.onestep.domain.route.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Schema(description = "산책 챌린지 응답 DTO")
public class RouteChallengeResponseDto {
    @Schema(description = "챌린지 세션 ID", example = "session-uuid-1234")
    private String challengeSessionId;

    @Schema(description = "산책 경로 세션 ID", example = "1")
    private Long routeSessionId;

    @Schema(description = "과제 상태 (PENDING, IN_PROGRESS, COMPLETED, FAILED)", example = "IN_PROGRESS")
    private String assignmentStatus;

    @Schema(description = "챌린지 시작 시간", example = "2025-01-27T10:30:00")
    private LocalDateTime startedAt;
}

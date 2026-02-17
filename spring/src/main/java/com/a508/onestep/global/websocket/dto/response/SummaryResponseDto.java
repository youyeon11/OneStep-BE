package com.a508.onestep.global.websocket.dto.response;

import com.a508.onestep.domain.room.entity.Solution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
WebSocket에서 요약 정보 전송에 사용하는 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponseDto {

    private Long solutionId;
    private Long roomId;
    private Long troubleId;
    private String summary;
    private LocalDateTime timestamp;

    public static SummaryResponseDto from(Solution solution, Long roomId) {
        return SummaryResponseDto.builder()
                .solutionId(solution.getId())
                .roomId(roomId)
                .troubleId(solution.getTroubleId())
                .summary(solution.getSummary())
                .timestamp(LocalDateTime.now())
                .build();
    }
}

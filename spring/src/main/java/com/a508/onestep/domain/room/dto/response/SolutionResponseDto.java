package com.a508.onestep.domain.room.dto.response;

import com.a508.onestep.domain.room.entity.Solution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolutionResponseDto {
    private Long solutionId;
    private Long troubleId;
    private String summary;
    private Boolean isRead;

    public static SolutionResponseDto from(Solution solution) {
        return SolutionResponseDto.builder()
                .solutionId(solution.getId())
                .troubleId(solution.getTroubleId())
                .summary(solution.getSummary())
                .isRead(solution.getReadAt() != null ? true : false)
                .build();
    }
}

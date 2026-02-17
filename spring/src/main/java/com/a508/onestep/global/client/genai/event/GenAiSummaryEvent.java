package com.a508.onestep.global.client.genai.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
GenAI 요약 요청 이벤트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenAiSummaryEvent {
    private Long roomId;
    private String userCode;
    private String roomSessionId;
    private String topic;
    private Long troubleId;
    private String summary;
}

package com.a508.onestep.global.message.dto;

import com.a508.onestep.domain.common.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StompMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 메시지 타입 (TOPIC, QUEUE, USER)
     */
    private MessageType type;

    /**
     * 목적지 경로
     */
    private String destination;

    private String userCode;

    /**
     * 실제 전송할 페이로드
     */
    private Object payload;

    /**
     * 추가 헤더 정보
     */
    private Map<String, Object> headers;

    /**
     * 메시지 생성 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 메시지 ID
     */
    private String messageId;
}

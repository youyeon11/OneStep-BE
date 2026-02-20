package com.a508.onestep.global.kafka.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaMessageDto<T> {
    private String id;
    private String type;
    private T payload;
    private Long timestamp;
}

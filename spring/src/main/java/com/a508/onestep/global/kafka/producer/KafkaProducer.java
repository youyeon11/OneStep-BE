package com.a508.onestep.global.kafka.producer;

import com.a508.onestep.global.kafka.dto.KafkaMessageDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
Kafka 메시지 발행 인터페이스
 */
public interface KafkaProducer {

    /*
    단일 메시지 동기 전송
     */
    <T> void send(String topic, String key, KafkaMessageDto<T> messageDto);

    /*
    단일 메시지 비동기 전송
     */
    <T> CompletableFuture<Void> sendAsync(String topic, String key, KafkaMessageDto<T> messageDto);

    /*
    메시지 키 없이 전송
     */
    <T> void send(String topic, KafkaMessageDto<T> messageDto);

    /*
    배치로 여러 메시지 전송
     */
    <T> void sendBatch(String topic, List<KafkaMessageDto<T>> messageDtoList);

    /*
    단순 문자열 메시지 전송
     */
    void send(String topic, String value);
}

package com.a508.onestep.global.kafka.service;

import com.a508.onestep.global.kafka.dto.KafkaMessageDto;
import com.a508.onestep.global.kafka.dto.UserFeatureSet;
import com.a508.onestep.global.kafka.producer.KafkaProducer;
import com.a508.onestep.global.kafka.topic.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KafkaServiceImpl implements KafkaService {

    private final KafkaProducer kafkaProducer;

    public void sendUserFeatureSet(UserFeatureSet featureSet) {

        KafkaMessageDto<UserFeatureSet> message = KafkaMessageDto.<UserFeatureSet>builder()
                .id(UUID.randomUUID().toString())
                .type("ROUTINE_GENERATE_REQUEST")
                .payload(featureSet)
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaProducer.sendAsync(
                KafkaTopics.USER_ROUTINE_GENERATE, featureSet.getUserCode(), message
        );
    }
}
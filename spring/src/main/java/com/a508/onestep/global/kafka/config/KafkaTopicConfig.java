package com.a508.onestep.global.kafka.config;

import com.a508.onestep.global.kafka.topic.KafkaTopics;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/*
Topic에 대한 설정
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9094}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /*
    Spring -> FastAPI
    TOPIC : user.routine.generate
    하루동안 보관하도록 설정
     */
    @Bean
    public NewTopic userRoutineGenerateTopic() {
        return TopicBuilder.name(KafkaTopics.USER_ROUTINE_GENERATE)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "86400000")
                .build();
    }

    /*
    방문 여부를 나타내는 userCode 전송
    TOPIC : visited.user.topic
    7일간 보관하도록 설정
     */
    @Bean
    public NewTopic visitedUserTopic() {
        return TopicBuilder.name(KafkaTopics.VISITED_USER)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    /*
    초기 유저의 정보 전달
     */
    @Bean
    public NewTopic initialUserTopic() {
        return TopicBuilder.name(KafkaTopics.INITIAL_USER_INFO)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "86400000")
                .config("cleanup.policy", "delete")
                .build();
    }

    /*
    Outbox 패턴 적용을 위한 토픽 설정
     */
    @Bean
    public NewTopic sendOutboxEvent() {
        return TopicBuilder.name(KafkaTopics.OUTBOX_EVENT)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "604800000")
                .config("cleanup.policy", "delete")
                .build();
    }
}

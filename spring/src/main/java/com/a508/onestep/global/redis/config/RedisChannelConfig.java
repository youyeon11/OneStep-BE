package com.a508.onestep.global.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.ChannelTopic;

@Configuration
public class RedisChannelConfig {

    /**
     * Redis Pub/Sub Topic 정의
     */
    @Bean
    public ChannelTopic topicChannel() {
        return new ChannelTopic("stomp:topic");
    }

    @Bean
    public ChannelTopic queueChannel() {
        return new ChannelTopic("stomp:queue");
    }

    @Bean
    public ChannelTopic userChannel() {
        return new ChannelTopic("stomp:user");
    }
}

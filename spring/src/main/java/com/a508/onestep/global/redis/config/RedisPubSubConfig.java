package com.a508.onestep.global.redis.config;

import com.a508.onestep.global.redis.subscribe.RedisMessageSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisMessageSubscriber redisMessageSubscriber;
    private final ChannelTopic topicChannel;
    private final ChannelTopic queueChannel;
    private final ChannelTopic userChannel;

    /**
     * Redis Message Listener Container
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);

        container.addMessageListener(
                (message, pattern) -> redisMessageSubscriber.handleTopicMessage(message, pattern),
                topicChannel
        );
        container.addMessageListener(
                (message, pattern) -> redisMessageSubscriber.handleQueueMessage(message, pattern),
                queueChannel
        );
        container.addMessageListener(
                (message, pattern) -> redisMessageSubscriber.handleUserMessage(message, pattern),
                userChannel
        );

        return container;
    }
}

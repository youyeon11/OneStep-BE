package com.a508.onestep.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Semaphore;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int connectionPool;

    @Bean
    public Semaphore databaseSemaphore() {
        int permits = (int) (connectionPool * 0.8);

        return new Semaphore(Math.max(1, permits));
    }
}

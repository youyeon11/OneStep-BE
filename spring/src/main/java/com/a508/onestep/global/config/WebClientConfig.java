package com.a508.onestep.global.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final String KAKAO_API_BASE_URL = "https://dapi.kakao.com";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int RESPONSE_TIMEOUT_SECONDS = 5;
    private static final int MAX_IN_MEMORY_SIZE = 2 * 1024 * 1024; // 2MB

    @Value("${kakao.api.key}")
    private String kakaoMapApiKey;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /*
    kakao Map WebClient
     */
    @Bean
    public WebClient kakaoMapWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(KAKAO_API_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoMapApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }
}

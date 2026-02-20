package com.a508.onestep.global.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleGenAiConfig {

    private static final String BASE_URL = "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com";

    @Bean

    public GoogleGenAiChatModel googleGenAiChatModel(
            @Value("${spring.ai.custom.google.genai.api-key}") String apiKey,
            @Value("${spring.ai.custom.google.genai.model}") String model
    ) {
        HttpOptions httpOptions = HttpOptions.builder()
                .baseUrl(BASE_URL)
                .build();

        Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(httpOptions)
                .build();

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(0.7)
                .build();
        return GoogleGenAiChatModel
                .builder()
                .genAiClient(client)
                .defaultOptions(options)
                .build();
    }


}

package com.a508.onestep;

import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {"com.a508.onestep"},
        exclude = { GoogleGenAiChatAutoConfiguration.class, S3AutoConfiguration.class }
)
public class OnestepApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnestepApplication.class, args);
    }

}

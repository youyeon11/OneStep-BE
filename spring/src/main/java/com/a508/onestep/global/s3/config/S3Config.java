package com.a508.onestep.global.s3.config;

import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {
    @Value("${cloud.aws.s3.access-key}")
    private String accessKey;

    @Value("${cloud.aws.s3.secret-key}")
    private String secretKey;

    private static String S3_REGION = "ap-northeast-2";

    @Bean
    @Primary
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(S3_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }
}

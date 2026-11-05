package com.jobtracker.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3Config {

    // 자격증명은 기본 체인(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)을 쓴다.
    @Bean
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder().region(Region.of(properties.region())).build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties properties) {
        return S3Presigner.builder().region(Region.of(properties.region())).build();
    }
}

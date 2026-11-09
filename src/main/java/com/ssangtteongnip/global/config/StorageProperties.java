package com.ssangtteongnip.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String bucket, String region, Duration presignTtl) {
}

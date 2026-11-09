package com.ssangtteongnip.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    // 기본 시간대는 SsangtteongnipApplication 에서 한국 시간으로 고정한다.
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

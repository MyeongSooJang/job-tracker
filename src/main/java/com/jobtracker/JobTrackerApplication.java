package com.jobtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class JobTrackerApplication {
    public static void main(String[] args) {
        // 서버 위치와 무관하게 '오늘' 판단(@PastOrPresent 등)을 한국 시간 기준으로 맞춘다
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(JobTrackerApplication.class, args);
    }
}

package com.ssangtteongnip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import java.util.TimeZone;

// 인증은 JWT 필터가 맡고 UserDetailsService를 쓰지 않으므로, 기동마다 임시 비밀번호를 만드는 자동 설정을 끈다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SsangtteongnipApplication {
    public static void main(String[] args) {
        // 서버 위치와 무관하게 '오늘' 판단(@PastOrPresent 등)을 한국 시간 기준으로 맞춘다
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(SsangtteongnipApplication.class, args);
    }
}

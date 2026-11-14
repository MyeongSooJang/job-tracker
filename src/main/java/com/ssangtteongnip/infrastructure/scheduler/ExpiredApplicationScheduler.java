package com.ssangtteongnip.infrastructure.scheduler;

import com.ssangtteongnip.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

// 스케줄링은 이 프로젝트에서 이 클래스만 쓰므로 활성화도 여기서 한다.
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ExpiredApplicationScheduler {

    private final ApplicationService applicationService;
    private final Clock clock;

    // 서버가 KST 00:00~05:50에 꺼지므로 그 이후 시각으로 잡는다.
    // 조회가 누적이라 하루를 놓쳐도 다음 실행에서 밀린 건을 함께 처리한다.
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public void rejectExpiredApplications() {
        int rejected = applicationService.rejectExpiredApplications(LocalDateTime.now(clock));
        log.info("마감일 + 1달이 지난 지원 {}건을 불합격 처리했다", rejected);
    }
}

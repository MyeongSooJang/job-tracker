package com.jobtracker.infrastructure.migration;

import com.jobtracker.application.service.ResumeMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 시작할 때마다 아직 S3로 옮기지 않은 이력서를 옮긴다. 옮길 건이 없으면 아무것도 하지 않는다.
// 이전이 끝나면(PR 2) 제거한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyResumeMigrationRunner implements ApplicationRunner {

    private final ResumeMigrationService migrationService;

    @Override
    public void run(ApplicationArguments args) {
        List<Long> pendingIds = migrationService.findPendingIds();
        log.info("이력서 S3 이전 대상 {}건", pendingIds.size());
        pendingIds.forEach(this::migrateQuietly);
    }

    private void migrateQuietly(Long resumeId) {
        try {
            migrationService.migrate(resumeId);
        } catch (RuntimeException e) {
            log.error("이력서 S3 이전 실패: id={}", resumeId, e);
        }
    }
}

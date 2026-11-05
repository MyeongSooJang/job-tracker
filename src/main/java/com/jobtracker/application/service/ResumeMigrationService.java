package com.jobtracker.application.service;

import com.jobtracker.domain.profile.FileStorage;
import com.jobtracker.domain.profile.ResumeFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// DB에 저장돼 있던 이력서를 S3로 옮기는 일회성 작업용이다. 이전이 끝나면(PR 2) 제거한다.
@Service
@RequiredArgsConstructor
public class ResumeMigrationService {

    private final ResumeFileRepository resumeFileRepository;
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public List<Long> findPendingIds() {
        return resumeFileRepository.findIdsNotMigrated();
    }

    // 건별 트랜잭션이다. 한 건이 실패해도 다른 건에 영향을 주지 않는다.
    @Transactional
    public void migrate(Long resumeId) {
        resumeFileRepository.findById(resumeId).ifPresent(resume -> resume.migrateTo(fileStorage));
    }
}

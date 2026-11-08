package com.jobtracker.domain.profile;

import java.util.Optional;

public interface ResumeFileRepository {
    ResumeFile save(ResumeFile resumeFile);
    Optional<ResumeFile> findById(Long id);
    Optional<ResumeFile> findByUserId(Long userId);
    Optional<ResumeSummary> findSummaryByUserId(Long userId);
    void delete(ResumeFile resumeFile);
}

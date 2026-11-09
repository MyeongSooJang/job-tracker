package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.profile.ResumeFile;
import com.ssangtteongnip.domain.profile.ResumeSummary;

import java.time.LocalDateTime;

public record ResumeResponse(String fileName, long sizeBytes, LocalDateTime updatedAt) {

    public static ResumeResponse from(ResumeSummary summary) {
        return new ResumeResponse(summary.fileName(), summary.sizeBytes(), summary.updatedAt());
    }

    public static ResumeResponse from(ResumeFile resume) {
        return new ResumeResponse(resume.getFileName(), resume.getSizeBytes(), resume.getUpdatedAt());
    }
}

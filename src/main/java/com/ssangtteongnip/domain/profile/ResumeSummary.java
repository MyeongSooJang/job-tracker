package com.ssangtteongnip.domain.profile;

import java.time.LocalDateTime;

public record ResumeSummary(String fileName, long sizeBytes, LocalDateTime updatedAt) {
}

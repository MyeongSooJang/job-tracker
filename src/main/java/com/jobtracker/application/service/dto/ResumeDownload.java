package com.jobtracker.application.service.dto;

import java.time.Instant;

public record ResumeDownload(String url, String fileName, Instant expiresAt) {}

package com.ssangtteongnip.application.service.dto;

import java.time.Instant;

public record FileDownload(String url, String fileName, Instant expiresAt) {}

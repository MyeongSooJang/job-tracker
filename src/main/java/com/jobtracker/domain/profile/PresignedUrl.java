package com.jobtracker.domain.profile;

import java.time.Instant;

public record PresignedUrl(String url, Instant expiresAt) {
}

package com.ssangtteongnip.domain.profile;

import java.time.Instant;

public record PresignedUrl(String url, Instant expiresAt) {
}

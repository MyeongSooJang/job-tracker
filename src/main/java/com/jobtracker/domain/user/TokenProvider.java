package com.jobtracker.domain.user;

public interface TokenProvider {
    String issue(Long userId);
    Long parseUserId(String token);
}

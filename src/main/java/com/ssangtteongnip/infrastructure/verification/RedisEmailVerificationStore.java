package com.ssangtteongnip.infrastructure.verification;

import com.ssangtteongnip.domain.user.EmailVerificationStore;
import com.ssangtteongnip.domain.user.VerificationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisEmailVerificationStore implements EmailVerificationStore {

    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    @Override
    public boolean tryAcquireCooldown(String email) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key("cooldown", email), "1", COOLDOWN_TTL));
    }

    @Override
    public void releaseCooldown(String email) {
        redis.delete(key("cooldown", email));
    }

    @Override
    public void saveCode(String email, VerificationCode code) {
        redis.opsForValue().set(key("code", email), code.value(), CODE_TTL);
        redis.delete(key("attempts", email));
    }

    @Override
    public Optional<VerificationCode> findCode(String email) {
        return Optional.ofNullable(redis.opsForValue().get(key("code", email))).map(VerificationCode::of);
    }

    @Override
    public long recordAttempt(String email) {
        String attemptsKey = key("attempts", email);
        long attempts = Objects.requireNonNull(redis.opsForValue().increment(attemptsKey));
        if (attempts == 1) {
            redis.expire(attemptsKey, CODE_TTL);
        }
        return attempts;
    }

    @Override
    public void deleteCode(String email) {
        redis.delete(List.of(key("code", email), key("attempts", email)));
    }

    @Override
    public void markVerified(String email) {
        redis.opsForValue().set(key("verified", email), "1", VERIFIED_TTL);
    }

    @Override
    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redis.hasKey(key("verified", email)));
    }

    @Override
    public void clearVerified(String email) {
        redis.delete(key("verified", email));
    }

    private String key(String kind, String email) {
        return "email-verify:" + kind + ":" + email;
    }
}

package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.user.EmailVerificationStore;
import com.ssangtteongnip.domain.user.VerificationCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FakeEmailVerificationStore implements EmailVerificationStore {

    private final Set<String> cooldowns = new HashSet<>();
    private final Map<String, VerificationCode> codes = new HashMap<>();
    private final Map<String, Long> attempts = new HashMap<>();
    private final Set<String> verified = new HashSet<>();

    @Override
    public boolean tryAcquireCooldown(String email) {
        return cooldowns.add(email);
    }

    @Override
    public void releaseCooldown(String email) {
        cooldowns.remove(email);
    }

    @Override
    public void saveCode(String email, VerificationCode code) {
        codes.put(email, code);
        attempts.remove(email);
    }

    @Override
    public Optional<VerificationCode> findCode(String email) {
        return Optional.ofNullable(codes.get(email));
    }

    @Override
    public long recordAttempt(String email) {
        return attempts.merge(email, 1L, Long::sum);
    }

    @Override
    public void deleteCode(String email) {
        codes.remove(email);
        attempts.remove(email);
    }

    @Override
    public void markVerified(String email) {
        verified.add(email);
    }

    @Override
    public boolean isVerified(String email) {
        return verified.contains(email);
    }

    @Override
    public void clearVerified(String email) {
        verified.remove(email);
    }
}

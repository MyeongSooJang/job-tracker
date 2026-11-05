package com.jobtracker.domain;

import com.jobtracker.domain.user.EmailVerificationStore;
import com.jobtracker.domain.user.VerificationCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FakeEmailVerificationStore implements EmailVerificationStore {

    private final Set<String> cooldowns = new HashSet<>();
    private final Map<String, VerificationCode> codes = new HashMap<>();
    private final Map<String, Long> failures = new HashMap<>();
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
        failures.remove(email);
    }

    @Override
    public Optional<VerificationCode> findCode(String email) {
        return Optional.ofNullable(codes.get(email));
    }

    @Override
    public long recordFailure(String email) {
        return failures.merge(email, 1L, Long::sum);
    }

    @Override
    public void deleteCode(String email) {
        codes.remove(email);
        failures.remove(email);
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

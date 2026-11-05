package com.jobtracker.domain.user;

import java.util.Optional;

public interface EmailVerificationStore {
    boolean tryAcquireCooldown(String email);
    void releaseCooldown(String email);
    // 새 코드를 저장하면 이전 코드와 틀린 횟수는 초기화된다.
    void saveCode(String email, VerificationCode code);
    Optional<VerificationCode> findCode(String email);
    // 틀린 횟수를 원자적으로 1 올리고 올린 뒤의 값을 반환한다.
    long recordFailure(String email);
    // 코드와 틀린 횟수를 함께 지운다.
    void deleteCode(String email);
    void markVerified(String email);
    boolean isVerified(String email);
    void clearVerified(String email);
}

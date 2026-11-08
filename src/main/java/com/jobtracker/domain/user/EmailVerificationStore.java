package com.jobtracker.domain.user;

import java.util.Optional;

public interface EmailVerificationStore {
    boolean tryAcquireCooldown(String email);
    void releaseCooldown(String email);
    // 새 코드를 저장하면 이전 코드와 시도 횟수는 초기화된다.
    void saveCode(String email, VerificationCode code);
    Optional<VerificationCode> findCode(String email);
    // 확인 시도 횟수를 원자적으로 1 올리고 올린 뒤의 값을 반환한다. 코드를 비교하기 전에 호출해야
    // 동시에 들어온 요청이 한도를 넘겨 비교되지 않는다.
    long recordAttempt(String email);
    // 코드와 시도 횟수를 함께 지운다.
    void deleteCode(String email);
    void markVerified(String email);
    boolean isVerified(String email);
    void clearVerified(String email);
}

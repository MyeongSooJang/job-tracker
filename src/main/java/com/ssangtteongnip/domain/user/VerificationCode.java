package com.ssangtteongnip.domain.user;

public final class VerificationCode {

    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final String value;

    private VerificationCode(String value) {
        this.value = value;
    }

    public static VerificationCode issue(CodeGenerator generator) {
        return new VerificationCode(generator.generate());
    }

    public static VerificationCode of(String value) {
        return new VerificationCode(value);
    }

    public boolean matches(String input) {
        return value.equals(input);
    }

    // attempt번째 시도가 허용 범위인지. 한도를 넘은 시도는 코드가 맞아도 비교하지 않는다.
    public boolean hasAttemptLeft(long attempt) {
        return attempt <= MAX_FAILED_ATTEMPTS;
    }

    public boolean isExhausted(long failedAttempts) {
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }

    // 저장소와 메일 발송이 값을 필요로 해서 노출한다.
    public String value() {
        return value;
    }
}

package com.jobtracker.domain;

import com.jobtracker.domain.user.VerificationCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodeTest {

    @Test
    void 생성기가_만든_값으로_코드를_발급한다() {
        VerificationCode code = VerificationCode.issue(() -> "123456");

        assertThat(code.value()).isEqualTo("123456");
    }

    @Test
    void 같은_값을_입력하면_일치한다() {
        assertThat(VerificationCode.of("123456").matches("123456")).isTrue();
    }

    @Test
    void 다른_값을_입력하면_일치하지_않는다() {
        assertThat(VerificationCode.of("123456").matches("654321")).isFalse();
    }

    @Test
    void 입력이_없으면_일치하지_않는다() {
        assertThat(VerificationCode.of("123456").matches(null)).isFalse();
    }

    @Test
    void 시도가_5번째까지는_허용되고_6번째부터는_거절된다() {
        VerificationCode code = VerificationCode.of("123456");

        assertThat(code.hasAttemptLeft(5)).isTrue();
        assertThat(code.hasAttemptLeft(6)).isFalse();
    }

    @Test
    void 틀린_횟수가_4회까지는_소진되지_않는다() {
        assertThat(VerificationCode.of("123456").isExhausted(4)).isFalse();
    }

    @Test
    void 틀린_횟수가_5회가_되면_소진된다() {
        assertThat(VerificationCode.of("123456").isExhausted(5)).isTrue();
    }
}

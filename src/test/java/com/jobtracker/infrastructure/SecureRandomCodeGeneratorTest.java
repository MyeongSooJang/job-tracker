package com.jobtracker.infrastructure;

import com.jobtracker.infrastructure.verification.SecureRandomCodeGenerator;
import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThat;

class SecureRandomCodeGeneratorTest {

    private final SecureRandomCodeGenerator generator = new SecureRandomCodeGenerator();

    @RepeatedTest(100)
    void 항상_6자리_숫자_코드를_만든다() {
        assertThat(generator.generate()).matches("\\d{6}");
    }
}

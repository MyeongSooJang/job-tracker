package com.jobtracker.infrastructure.verification;

import com.jobtracker.domain.user.CodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureRandomCodeGenerator implements CodeGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}

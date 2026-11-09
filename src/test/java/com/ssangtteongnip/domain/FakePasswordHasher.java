package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.user.PasswordHasher;

public class FakePasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return hash(rawPassword).equals(passwordHash);
    }
}

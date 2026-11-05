package com.jobtracker.application.service.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email) {}

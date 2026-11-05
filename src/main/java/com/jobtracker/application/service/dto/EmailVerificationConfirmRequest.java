package com.jobtracker.application.service.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email,
        @NotBlank(message = "인증 코드를 입력해주세요.") String code) {}

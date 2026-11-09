package com.ssangtteongnip.application.service.dto;

import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email,
        @NotBlank(message = "이름을 입력해주세요.") String name,
        @NotBlank(message = "비밀번호를 입력해주세요.") String password) {}

package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.AuthService;
import com.jobtracker.application.service.EmailVerificationService;
import com.jobtracker.application.service.dto.EmailVerificationConfirmRequest;
import com.jobtracker.application.service.dto.EmailVerificationRequest;
import com.jobtracker.application.service.dto.LoginRequest;
import com.jobtracker.application.service.dto.SignupRequest;
import com.jobtracker.application.service.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email-verifications")
    public void sendVerificationCode(@RequestBody @Valid EmailVerificationRequest request) {
        emailVerificationService.send(request.email());
    }

    @PostMapping("/email-verifications/confirm")
    public void confirmVerificationCode(@RequestBody @Valid EmailVerificationConfirmRequest request) {
        emailVerificationService.confirm(request.email(), request.code());
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@RequestBody @Valid SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }
}

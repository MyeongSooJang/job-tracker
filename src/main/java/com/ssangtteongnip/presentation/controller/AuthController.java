package com.ssangtteongnip.presentation.controller;

import com.ssangtteongnip.application.service.AuthService;
import com.ssangtteongnip.application.service.EmailVerificationService;
import com.ssangtteongnip.application.service.dto.EmailVerificationConfirmRequest;
import com.ssangtteongnip.application.service.dto.EmailVerificationRequest;
import com.ssangtteongnip.application.service.dto.LoginRequest;
import com.ssangtteongnip.application.service.dto.SignupRequest;
import com.ssangtteongnip.application.service.dto.TokenResponse;
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

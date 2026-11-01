package com.jobtracker.application;

import com.jobtracker.application.service.AuthService;
import com.jobtracker.application.service.dto.LoginRequest;
import com.jobtracker.application.service.dto.SignupRequest;
import com.jobtracker.application.service.dto.TokenResponse;
import com.jobtracker.domain.FakePasswordHasher;
import com.jobtracker.domain.profile.ProfileRepository;
import com.jobtracker.domain.user.PasswordHasher;
import com.jobtracker.domain.user.TokenProvider;
import com.jobtracker.domain.user.User;
import com.jobtracker.domain.user.UserRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock ProfileRepository profileRepository;
    @Mock TokenProvider tokenProvider;

    private final PasswordHasher passwordHasher = new FakePasswordHasher();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, profileRepository, passwordHasher, tokenProvider);
    }

    @Test
    void signup_회원가입하면_프로필도_함께_생성하고_토큰을_발급한다() {
        given(userRepository.existsByEmail("hong@example.com")).willReturn(false);
        given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(tokenProvider.issue(any())).willReturn("jwt-token");

        TokenResponse response = authService.signup(new SignupRequest(" Hong@Example.com ", "홍길동", "password1"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        verify(profileRepository).save(any());
    }

    @Test
    void signup_이미_가입된_이메일이면_예외가_발생한다() {
        given(userRepository.existsByEmail("hong@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("Hong@Example.com", "홍길동", "password1")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
        verify(userRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void login_올바른_자격_증명이면_토큰을_발급한다() {
        User user = User.register("hong@example.com", "홍길동", "password1", passwordHasher);
        given(userRepository.findByEmail("hong@example.com")).willReturn(Optional.of(user));
        given(tokenProvider.issue(any())).willReturn("jwt-token");

        TokenResponse response = authService.login(new LoginRequest("HONG@example.com", "password1"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_없는_이메일이면_자격_증명_오류가_발생한다() {
        given(userRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("none@example.com", "password1")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void login_비밀번호가_틀리면_토큰을_발급하지_않는다() {
        User user = User.register("hong@example.com", "홍길동", "password1", passwordHasher);
        given(userRepository.findByEmail("hong@example.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("hong@example.com", "wrong-password")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        verify(tokenProvider, never()).issue(any());
    }
}

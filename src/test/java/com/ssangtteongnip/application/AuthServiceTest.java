package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.AuthService;
import com.ssangtteongnip.application.service.dto.LoginRequest;
import com.ssangtteongnip.application.service.dto.SignupRequest;
import com.ssangtteongnip.application.service.dto.TokenResponse;
import com.ssangtteongnip.domain.FakeEmailVerificationStore;
import com.ssangtteongnip.domain.FakePasswordHasher;
import com.ssangtteongnip.domain.profile.ProfileRepository;
import com.ssangtteongnip.domain.user.PasswordHasher;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
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
    private final FakeEmailVerificationStore verificationStore = new FakeEmailVerificationStore();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, profileRepository, passwordHasher, tokenProvider,
                verificationStore);
    }

    @Test
    void signup_회원가입하면_프로필도_함께_생성하고_토큰을_발급한다() {
        verificationStore.markVerified("hong@example.com");
        given(userRepository.existsByEmail("hong@example.com")).willReturn(false);
        given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(tokenProvider.issue(any())).willReturn("jwt-token");

        TokenResponse response = authService.signup(new SignupRequest(" Hong@Example.com ", "홍길동", "password1"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        verify(profileRepository).save(any());
    }

    @Test
    void signup_가입에_성공하면_인증_완료_상태를_지운다() {
        verificationStore.markVerified("hong@example.com");
        given(userRepository.existsByEmail("hong@example.com")).willReturn(false);
        given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(tokenProvider.issue(any())).willReturn("jwt-token");

        authService.signup(new SignupRequest("hong@example.com", "홍길동", "password1"));

        assertThat(verificationStore.isVerified("hong@example.com")).isFalse();
    }

    @Test
    void signup_이메일_인증을_받지_않았으면_가입할_수_없다() {
        given(userRepository.existsByEmail("hong@example.com")).willReturn(false);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("hong@example.com", "홍길동", "password1")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));
        verify(userRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
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

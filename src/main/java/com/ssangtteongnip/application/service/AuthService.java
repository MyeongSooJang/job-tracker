package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.LoginRequest;
import com.ssangtteongnip.application.service.dto.SignupRequest;
import com.ssangtteongnip.application.service.dto.TokenResponse;
import com.ssangtteongnip.domain.profile.Profile;
import com.ssangtteongnip.domain.profile.ProfileRepository;
import com.ssangtteongnip.domain.user.EmailVerificationStore;
import com.ssangtteongnip.domain.user.PasswordHasher;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.global.transaction.AfterCommit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final EmailVerificationStore verificationStore;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (!verificationStore.isVerified(email)) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        User user = userRepository.save(
                User.register(request.email(), request.name(), request.password(), passwordHasher));
        profileRepository.save(Profile.create(user));
        AfterCommit.runQuietly("이메일 인증 상태 삭제", () -> verificationStore.clearVerified(email));
        return new TokenResponse(tokenProvider.issue(user.getId()));
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        user.authenticate(request.password(), passwordHasher);
        return new TokenResponse(tokenProvider.issue(user.getId()));
    }
}

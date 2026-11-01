package com.jobtracker.application.service;

import com.jobtracker.application.service.dto.LoginRequest;
import com.jobtracker.application.service.dto.SignupRequest;
import com.jobtracker.application.service.dto.TokenResponse;
import com.jobtracker.domain.profile.Profile;
import com.jobtracker.domain.profile.ProfileRepository;
import com.jobtracker.domain.user.PasswordHasher;
import com.jobtracker.domain.user.TokenProvider;
import com.jobtracker.domain.user.User;
import com.jobtracker.domain.user.UserRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
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

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(User.normalizeEmail(request.email()))) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = userRepository.save(
                User.register(request.email(), request.name(), request.password(), passwordHasher));
        profileRepository.save(Profile.create(user));
        return new TokenResponse(tokenProvider.issue(user.getId()));
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        user.authenticate(request.password(), passwordHasher);
        return new TokenResponse(tokenProvider.issue(user.getId()));
    }
}

package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.ProfileRequest;
import com.ssangtteongnip.application.service.dto.ProfileResponse;
import com.ssangtteongnip.application.service.dto.UserFileResponse;
import com.ssangtteongnip.domain.profile.Profile;
import com.ssangtteongnip.domain.profile.ProfileRepository;
import com.ssangtteongnip.domain.profile.UserFileRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserFileRepository userFileRepository;

    public ProfileResponse getMyProfile(Long userId) {
        return toResponse(userId, findProfile(userId));
    }

    @Transactional
    public ProfileResponse reviseMyProfile(Long userId, ProfileRequest request) {
        Profile profile = findProfile(userId);
        profile.reviseNameAndInfo(request.name(), request.toPersonalInfo());
        return toResponse(userId, profile);
    }

    private Profile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
    }

    private ProfileResponse toResponse(Long userId, Profile profile) {
        List<UserFileResponse> files = userFileRepository.findUserFilesNewestFirst(userId).stream()
                .map(UserFileResponse::describe)
                .toList();
        return ProfileResponse.of(profile, files);
    }
}

package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.ProfileService;
import com.jobtracker.application.service.dto.ProfileRequest;
import com.jobtracker.application.service.dto.ProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponse getMyProfile(@AuthenticationPrincipal Long userId) {
        return profileService.getMyProfile(userId);
    }

    @PutMapping
    public ProfileResponse updateMyProfile(@AuthenticationPrincipal Long userId,
                                           @RequestBody @Valid ProfileRequest request) {
        return profileService.updateMyProfile(userId, request);
    }
}

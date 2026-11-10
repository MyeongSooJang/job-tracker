package com.ssangtteongnip.presentation.controller;

import com.ssangtteongnip.application.service.StudyImageService;
import com.ssangtteongnip.application.service.dto.StudyImageResponse;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StudyImageController {

    // 리다이렉트를 브라우저가 잠깐 재사용하되, presigned URL이 만료되기 전에 다시 받도록 짧게 둔다.
    private static final Duration REDIRECT_CACHE = Duration.ofMinutes(1);

    private final StudyImageService studyImageService;

    @PostMapping(value = "/me/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StudyImageResponse upload(@AuthenticationPrincipal Long userId,
                                     @RequestPart("file") MultipartFile file) throws IOException {
        return studyImageService.upload(userId, file.getBytes());
    }

    // <img src>는 Authorization 헤더를 보낼 수 없으므로 인증 없이 열어 둔다. 주소(UUID)를 아는 사람만 볼 수 있다.
    @GetMapping("/images/{publicId}")
    public ResponseEntity<Void> view(@PathVariable UUID publicId) {
        PresignedUrl link = studyImageService.view(publicId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, link.url())
                .cacheControl(CacheControl.maxAge(REDIRECT_CACHE).cachePrivate())
                .build();
    }
}

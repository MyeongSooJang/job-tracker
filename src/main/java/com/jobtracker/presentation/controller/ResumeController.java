package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.ResumeService;
import com.jobtracker.application.service.dto.ResumeDownload;
import com.jobtracker.application.service.dto.ResumeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/me/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeResponse upload(@AuthenticationPrincipal Long userId,
                                 @RequestPart("file") MultipartFile file) throws IOException {
        return resumeService.upload(userId, file.getOriginalFilename(), file.getBytes());
    }

    @GetMapping
    public ResumeDownload download(@AuthenticationPrincipal Long userId) {
        return resumeService.download(userId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId) {
        resumeService.delete(userId);
    }
}

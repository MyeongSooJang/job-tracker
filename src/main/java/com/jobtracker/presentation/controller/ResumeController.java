package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.ResumeService;
import com.jobtracker.application.service.dto.ResumeDownload;
import com.jobtracker.application.service.dto.ResumeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal Long userId) {
        ResumeDownload resume = resumeService.download(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(resume.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resume.content());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId) {
        resumeService.delete(userId);
    }
}

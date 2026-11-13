package com.ssangtteongnip.presentation.controller;

import com.ssangtteongnip.application.service.UserFileService;
import com.ssangtteongnip.application.service.dto.FileDownload;
import com.ssangtteongnip.application.service.dto.UserFileResponse;
import com.ssangtteongnip.domain.profile.FileType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/me/files")
@RequiredArgsConstructor
public class UserFileController {

    private final UserFileService userFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UserFileResponse upload(@AuthenticationPrincipal Long userId,
                                   @RequestParam(value = "type", required = false) String type,
                                   @RequestPart("file") MultipartFile file) throws IOException {
        return userFileService.upload(userId, FileType.parse(type), file.getOriginalFilename(), file.getBytes());
    }

    @GetMapping
    public List<UserFileResponse> list(@AuthenticationPrincipal Long userId) {
        return userFileService.list(userId);
    }

    @GetMapping("/{id}")
    public FileDownload download(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return userFileService.download(userId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        userFileService.delete(userId, id);
    }
}

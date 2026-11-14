package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.domain.profile.UserFile;

import java.time.LocalDateTime;

public record UserFileResponse(Long id, FileType type, String fileName, long sizeBytes, LocalDateTime createdAt) {

    public static UserFileResponse describe(UserFile file) {
        return new UserFileResponse(file.getId(), file.getType(), file.getFileName(),
                file.getSizeBytes(), file.getCreatedAt());
    }
}

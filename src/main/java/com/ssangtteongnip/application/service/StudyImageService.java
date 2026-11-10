package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.StudyImageResponse;
import com.ssangtteongnip.domain.image.StudyImage;
import com.ssangtteongnip.domain.image.StudyImageRepository;
import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyImageService {

    private final StudyImageRepository studyImageRepository;
    private final FileStorage fileStorage;

    @Transactional
    public StudyImageResponse upload(Long userId, byte[] content) {
        StudyImage image = studyImageRepository.save(StudyImage.upload(userId, content, fileStorage));
        return StudyImageResponse.from(image);
    }

    public PresignedUrl view(UUID publicId) {
        return studyImageRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND))
                .viewUrl(fileStorage);
    }
}

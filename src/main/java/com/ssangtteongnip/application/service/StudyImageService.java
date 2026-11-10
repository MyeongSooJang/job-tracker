package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.StudyImageResponse;
import com.ssangtteongnip.domain.image.StudyImage;
import com.ssangtteongnip.domain.image.StudyImageRepository;
import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.studylog.StudyLogRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.global.transaction.AfterCommit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyImageService {

    private final StudyImageRepository studyImageRepository;
    private final StudyLogRepository studyLogRepository;
    private final FileStorage fileStorage;

    @Transactional
    public StudyImageResponse upload(Long userId, byte[] content) {
        StudyImage image = studyImageRepository.save(StudyImage.upload(userId, content, fileStorage));
        return StudyImageResponse.from(image);
    }

    // 글에서 빠졌거나 글과 함께 사라진 이미지 중, 아무 글도 더는 쓰지 않는 내 이미지를 지운다.
    // 남의 이미지는 아이디를 알아도 지울 수 없고, 다른 글이 아직 쓰는 이미지(글을 복사한 경우 등)는 남긴다.
    // 호출하는 쪽의 변경(글 삭제·수정)이 같은 트랜잭션에서 먼저 반영되어 있어야 한다.
    @Transactional
    public void deleteUnused(Long userId, Set<UUID> candidates) {
        for (UUID publicId : candidates) {
            studyImageRepository.findByPublicId(publicId)
                    .filter(image -> image.isOwnedBy(userId))
                    .filter(image -> !studyLogRepository.existsByContentContaining(image.publicPath()))
                    .ifPresent(this::delete);
        }
    }

    // DB 행은 이 트랜잭션에서 지우고, S3 객체는 커밋된 뒤에 지운다. 객체 삭제가 실패해도 글 삭제는 성공시킨다.
    private void delete(StudyImage image) {
        studyImageRepository.delete(image);
        AfterCommit.runQuietly("학습 이미지 객체 삭제", () -> image.deleteFrom(fileStorage));
    }

    public PresignedUrl view(UUID publicId) {
        return studyImageRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND))
                .viewUrl(fileStorage);
    }
}

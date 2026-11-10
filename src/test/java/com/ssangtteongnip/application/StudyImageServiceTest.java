package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.StudyImageService;
import com.ssangtteongnip.application.service.dto.StudyImageResponse;
import com.ssangtteongnip.domain.FakeFileStorage;
import com.ssangtteongnip.domain.image.StudyImage;
import com.ssangtteongnip.domain.image.StudyImageRepository;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudyImageServiceTest {

    private static final Long USER_ID = 1L;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Mock StudyImageRepository studyImageRepository;

    private final FakeFileStorage storage = new FakeFileStorage();
    private StudyImageService studyImageService;

    @BeforeEach
    void setUp() {
        studyImageService = new StudyImageService(studyImageRepository, storage);
    }

    @Test
    void 이미지를_올리면_저장소에_올리고_본문에_넣을_경로를_돌려준다() {
        given(studyImageRepository.save(any(StudyImage.class))).willAnswer(i -> i.getArgument(0));

        StudyImageResponse response = studyImageService.upload(USER_ID, PNG);

        assertThat(response.url()).startsWith("/api/images/");
        assertThat(storage.size()).isEqualTo(1);
    }

    @Test
    void 이미지가_아니면_저장소에도_DB에도_남기지_않는다() {
        assertErrorCode(() -> studyImageService.upload(USER_ID, "text".getBytes()), ErrorCode.INVALID_IMAGE_TYPE);

        assertThat(storage.size()).isZero();
        verify(studyImageRepository, never()).save(any());
    }

    @Test
    void 공개_아이디로_보여_줄_주소를_찾는다() {
        StudyImage image = StudyImage.upload(USER_ID, PNG, storage);
        given(studyImageRepository.findByPublicId(image.getPublicId())).willReturn(Optional.of(image));

        PresignedUrl link = studyImageService.view(image.getPublicId());

        assertThat(link.url()).contains(image.getObjectKey());
    }

    @Test
    void 없는_이미지는_찾을_수_없다() {
        UUID unknown = UUID.randomUUID();
        given(studyImageRepository.findByPublicId(unknown)).willReturn(Optional.empty());

        assertErrorCode(() -> studyImageService.view(unknown), ErrorCode.IMAGE_NOT_FOUND);
    }
}

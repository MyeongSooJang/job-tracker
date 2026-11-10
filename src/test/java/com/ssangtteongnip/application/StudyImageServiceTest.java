package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.StudyImageService;
import com.ssangtteongnip.application.service.dto.StudyImageResponse;
import com.ssangtteongnip.domain.FakeFileStorage;
import com.ssangtteongnip.domain.image.StudyImage;
import com.ssangtteongnip.domain.image.StudyImageRepository;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.studylog.StudyLogRepository;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StudyImageServiceTest {

    private static final Long USER_ID = 1L;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Mock StudyImageRepository studyImageRepository;
    @Mock StudyLogRepository studyLogRepository;

    private final FakeFileStorage storage = new FakeFileStorage();
    private StudyImageService studyImageService;

    @BeforeEach
    void setUp() {
        studyImageService = new StudyImageService(studyImageRepository, studyLogRepository, storage);
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

    private StudyImage uploaded(Long ownerId) {
        StudyImage image = StudyImage.upload(ownerId, PNG, storage);
        given(studyImageRepository.findByPublicId(image.getPublicId())).willReturn(Optional.of(image));
        return image;
    }

    @Test
    void 아무_글도_쓰지_않는_내_이미지는_행과_저장소_객체를_모두_지운다() {
        StudyImage image = uploaded(USER_ID);
        given(studyLogRepository.existsByContentContaining(image.publicPath())).willReturn(false);

        studyImageService.deleteUnused(USER_ID, Set.of(image.getPublicId()));

        verify(studyImageRepository).delete(image);
        assertThat(storage.contains(image.getObjectKey())).isFalse();
    }

    @Test
    void 다른_글이_아직_쓰는_이미지는_지우지_않는다() {
        StudyImage image = uploaded(USER_ID);
        given(studyLogRepository.existsByContentContaining(image.publicPath())).willReturn(true);

        studyImageService.deleteUnused(USER_ID, Set.of(image.getPublicId()));

        verify(studyImageRepository, never()).delete(any());
        assertThat(storage.contains(image.getObjectKey())).isTrue();
    }

    @Test
    void 남의_이미지는_아이디를_본문에_넣어도_지울_수_없다() {
        StudyImage othersImage = uploaded(USER_ID + 1);

        studyImageService.deleteUnused(USER_ID, Set.of(othersImage.getPublicId()));

        verify(studyImageRepository, never()).delete(any());
        verifyNoInteractions(studyLogRepository);
        assertThat(storage.contains(othersImage.getObjectKey())).isTrue();
    }

    @Test
    void 없는_이미지_아이디는_무시한다() {
        UUID unknown = UUID.randomUUID();
        given(studyImageRepository.findByPublicId(unknown)).willReturn(Optional.empty());

        studyImageService.deleteUnused(USER_ID, Set.of(unknown));

        verify(studyImageRepository, never()).delete(any());
    }

    @Test
    void 여러_이미지_중_아무_글도_쓰지_않는_것만_지운다() {
        StudyImage unused = uploaded(USER_ID);
        StudyImage stillUsed = uploaded(USER_ID);
        given(studyLogRepository.existsByContentContaining(unused.publicPath())).willReturn(false);
        given(studyLogRepository.existsByContentContaining(stillUsed.publicPath())).willReturn(true);

        studyImageService.deleteUnused(USER_ID, new LinkedHashSet<>(List.of(unused.getPublicId(), stillUsed.getPublicId())));

        verify(studyImageRepository).delete(unused);
        verify(studyImageRepository, never()).delete(stillUsed);
        assertThat(storage.contains(unused.getObjectKey())).isFalse();
        assertThat(storage.contains(stillUsed.getObjectKey())).isTrue();
    }

    @Test
    void 저장소_객체_삭제가_실패해도_예외로_번지지_않고_행은_지운다() {
        StudyImage image = uploaded(USER_ID);
        given(studyLogRepository.existsByContentContaining(image.publicPath())).willReturn(false);
        storage.failOnDelete();

        studyImageService.deleteUnused(USER_ID, Set.of(image.getPublicId()));

        verify(studyImageRepository).delete(image);
    }

    @Test
    void 지울_후보가_없으면_아무것도_조회하지_않는다() {
        studyImageService.deleteUnused(USER_ID, Set.of());

        verifyNoInteractions(studyImageRepository, studyLogRepository);
    }

    @Test
    void 없는_이미지는_찾을_수_없다() {
        UUID unknown = UUID.randomUUID();
        given(studyImageRepository.findByPublicId(unknown)).willReturn(Optional.empty());

        assertErrorCode(() -> studyImageService.view(unknown), ErrorCode.IMAGE_NOT_FOUND);
    }
}

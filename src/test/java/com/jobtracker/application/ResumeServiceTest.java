package com.jobtracker.application;

import com.jobtracker.application.service.ResumeService;
import com.jobtracker.application.service.dto.ResumeDownload;
import com.jobtracker.application.service.dto.ResumeResponse;
import com.jobtracker.domain.FakeFileStorage;
import com.jobtracker.domain.FakePasswordHasher;
import com.jobtracker.domain.profile.ResumeFile;
import com.jobtracker.domain.profile.ResumeFileRepository;
import com.jobtracker.domain.user.User;
import com.jobtracker.domain.user.UserRepository;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    private static final Long USER_ID = 1L;

    @Mock ResumeFileRepository resumeFileRepository;
    @Mock UserRepository userRepository;

    private final FakeFileStorage storage = new FakeFileStorage();
    private final User user = registeredUser();
    private ResumeService resumeService;

    private static User registeredUser() {
        User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private static byte[] pdf(String body) {
        return ("%PDF-1.7\n" + body).getBytes(StandardCharsets.US_ASCII);
    }

    @BeforeEach
    void setUp() {
        resumeService = new ResumeService(resumeFileRepository, userRepository, storage);
    }

    @Test
    void 이력서가_없으면_저장소에_올리고_새로_저장한다() {
        byte[] content = pdf("resume");
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(resumeFileRepository.save(any(ResumeFile.class))).willAnswer(i -> i.getArgument(0));

        ResumeResponse response = resumeService.upload(USER_ID, "이력서.pdf", content);

        assertThat(response.fileName()).isEqualTo("이력서.pdf");
        assertThat(response.sizeBytes()).isEqualTo(content.length);
        assertThat(storage.size()).isEqualTo(1);
    }

    @Test
    void 이력서가_이미_있으면_새로_저장하지_않고_교체하며_이전_객체를_지운다() {
        ResumeFile existing = ResumeFile.upload(user, "old.pdf", pdf("old"), storage);
        String oldKey = existing.getObjectKey();
        byte[] newContent = pdf("new content");
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.of(existing));

        ResumeResponse response = resumeService.upload(USER_ID, "new.pdf", newContent);

        assertThat(response.fileName()).isEqualTo("new.pdf");
        assertThat(storage.contentOf(existing.getObjectKey())).isEqualTo(newContent);
        assertThat(storage.contains(oldKey)).isFalse();
        assertThat(storage.size()).isEqualTo(1);
        verify(resumeFileRepository, never()).save(any());
    }

    @Test
    void 이전_객체를_지우지_못해도_교체는_성공한다() {
        ResumeFile existing = ResumeFile.upload(user, "old.pdf", pdf("old"), storage);
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.of(existing));
        storage.failOnDelete();

        ResumeResponse response = resumeService.upload(USER_ID, "new.pdf", pdf("new"));

        assertThat(response.fileName()).isEqualTo("new.pdf");
    }

    @Test
    void 저장소에_올리지_못하면_기존_이력서를_유지한다() {
        ResumeFile existing = ResumeFile.upload(user, "old.pdf", pdf("old"), storage);
        String oldKey = existing.getObjectKey();
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.of(existing));
        storage.failOnPut();

        assertErrorCode(() -> resumeService.upload(USER_ID, "new.pdf", pdf("new")), ErrorCode.STORAGE_FAILED);

        assertThat(existing.getFileName()).isEqualTo("old.pdf");
        assertThat(storage.contains(oldKey)).isTrue();
    }

    @Test
    void 가입하지_않은_사용자는_이력서를_올릴_수_없다() {
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> resumeService.upload(USER_ID, "이력서.pdf", pdf("resume")), ErrorCode.USER_NOT_FOUND);
        verify(resumeFileRepository, never()).save(any());
        assertThat(storage.size()).isZero();
    }

    @Test
    void 올린_이력서는_파일명과_내려받기_주소로_내려받을_수_있다() {
        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", pdf("resume"), storage);
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.of(resume));

        ResumeDownload download = resumeService.download(USER_ID);

        assertThat(download.fileName()).isEqualTo("이력서.pdf");
        assertThat(download.url()).contains(resume.getObjectKey());
        assertThat(download.expiresAt()).isEqualTo(FakeFileStorage.EXPIRES_AT);
    }

    @Test
    void 이력서가_없으면_내려받을_수_없다() {
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> resumeService.download(USER_ID), ErrorCode.RESUME_NOT_FOUND);
    }

    @Test
    void 이력서를_삭제하면_저장소에서_제거되고_객체도_지워진다() {
        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", pdf("resume"), storage);
        String key = resume.getObjectKey();
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.of(resume));

        resumeService.delete(USER_ID);

        verify(resumeFileRepository).delete(resume);
        assertThat(storage.contains(key)).isFalse();
    }

    @Test
    void 이력서가_없으면_삭제할_수_없다() {
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> resumeService.delete(USER_ID), ErrorCode.RESUME_NOT_FOUND);
        verify(resumeFileRepository, never()).delete(any());
    }
}

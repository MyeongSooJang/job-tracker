package com.jobtracker.application;

import com.jobtracker.application.service.ResumeService;
import com.jobtracker.application.service.dto.ResumeDownload;
import com.jobtracker.application.service.dto.ResumeResponse;
import com.jobtracker.domain.FakePasswordHasher;
import com.jobtracker.domain.profile.ResumeFile;
import com.jobtracker.domain.profile.ResumeFileRepository;
import com.jobtracker.domain.user.User;
import com.jobtracker.domain.user.UserRepository;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @InjectMocks ResumeService resumeService;

    private final User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());

    private static byte[] pdf(String body) {
        return ("%PDF-1.7\n" + body).getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void 이력서가_없으면_새로_저장한다() {
        byte[] content = pdf("resume");
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(resumeFileRepository.save(any(ResumeFile.class))).willAnswer(i -> i.getArgument(0));

        ResumeResponse response = resumeService.upload(USER_ID, "이력서.pdf", content);

        assertThat(response.fileName()).isEqualTo("이력서.pdf");
        assertThat(response.sizeBytes()).isEqualTo(content.length);
    }

    @Test
    void 이력서가_이미_있으면_새로_저장하지_않고_기존_파일을_교체한다() {
        ResumeFile existing = ResumeFile.upload(user, "old.pdf", pdf("old"));
        byte[] newContent = pdf("new content");
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.of(existing));

        ResumeResponse response = resumeService.upload(USER_ID, "new.pdf", newContent);

        assertThat(response.fileName()).isEqualTo("new.pdf");
        assertThat(existing.getContent()).isEqualTo(newContent);
        verify(resumeFileRepository, never()).save(any());
    }

    @Test
    void 가입하지_않은_사용자는_이력서를_올릴_수_없다() {
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> resumeService.upload(USER_ID, "이력서.pdf", pdf("resume")), ErrorCode.USER_NOT_FOUND);
        verify(resumeFileRepository, never()).save(any());
    }

    @Test
    void 올린_이력서는_파일명과_내용으로_내려받을_수_있다() {
        byte[] content = pdf("resume");
        given(resumeFileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(ResumeFile.upload(user, "이력서.pdf", content)));

        ResumeDownload download = resumeService.download(USER_ID);

        assertThat(download.fileName()).isEqualTo("이력서.pdf");
        assertThat(download.content()).isEqualTo(content);
    }

    @Test
    void 이력서가_없으면_내려받을_수_없다() {
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> resumeService.download(USER_ID), ErrorCode.RESUME_NOT_FOUND);
    }

    @Test
    void 이력서를_삭제하면_저장소에서_제거된다() {
        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", pdf("resume"));
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.of(resume));

        resumeService.delete(USER_ID);

        verify(resumeFileRepository).delete(resume);
    }

    @Test
    void 이력서가_없으면_삭제할_수_없다() {
        given(resumeFileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> resumeService.delete(USER_ID), ErrorCode.RESUME_NOT_FOUND);
        verify(resumeFileRepository, never()).delete(any());
    }
}

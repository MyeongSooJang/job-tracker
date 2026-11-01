package com.jobtracker.domain;

import com.jobtracker.domain.profile.ResumeFile;
import com.jobtracker.domain.user.User;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeFileTest {

    private final User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());

    private static byte[] pdf(String body) {
        return ("%PDF-1.7\n" + body).getBytes(StandardCharsets.US_ASCII);
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void upload_PDF_파일을_등록한다() {
        byte[] content = pdf("resume");

        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", content);

        assertThat(resume.getFileName()).isEqualTo("이력서.pdf");
        assertThat(resume.getSizeBytes()).isEqualTo(content.length);
        assertThat(resume.getContent()).isEqualTo(content);
    }

    @Test
    void upload_파일명에서_경로를_제거한다() {
        ResumeFile resume = ResumeFile.upload(user, "C:\\Users\\hong\\이력서.pdf", pdf("x"));

        assertThat(resume.getFileName()).isEqualTo("이력서.pdf");
    }

    @Test
    void upload_파일명이_없으면_기본_이름을_사용한다() {
        ResumeFile resume = ResumeFile.upload(user, " ", pdf("x"));

        assertThat(resume.getFileName()).isEqualTo("resume.pdf");
    }

    @Test
    void upload_PDF가_아니면_예외가_발생한다() {
        byte[] notPdf = "plain text".getBytes(StandardCharsets.US_ASCII);

        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", notPdf), ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void upload_빈_파일이면_예외가_발생한다() {
        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", new byte[0]), ErrorCode.EMPTY_FILE);
    }

    @Test
    void upload_5MB를_초과하면_예외가_발생한다() {
        byte[] tooLarge = new byte[ResumeFile.MAX_SIZE_BYTES + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, tooLarge, 0, 5);

        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", tooLarge), ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void replace_기존_파일을_새_파일로_교체한다() {
        ResumeFile resume = ResumeFile.upload(user, "old.pdf", pdf("old"));
        byte[] newContent = pdf("new content");

        resume.replace("new.pdf", newContent);

        assertThat(resume.getFileName()).isEqualTo("new.pdf");
        assertThat(resume.getSizeBytes()).isEqualTo(newContent.length);
        assertThat(resume.getContent()).isEqualTo(newContent);
    }

    @Test
    void replace_검증에_실패하면_기존_파일을_유지한다() {
        byte[] original = pdf("old");
        ResumeFile resume = ResumeFile.upload(user, "old.pdf", original);

        assertErrorCode(() -> resume.replace("bad.pdf", "nope".getBytes(StandardCharsets.US_ASCII)),
                ErrorCode.INVALID_FILE_TYPE);

        assertThat(resume.getFileName()).isEqualTo("old.pdf");
        assertThat(resume.getContent()).isEqualTo(original);
    }
}

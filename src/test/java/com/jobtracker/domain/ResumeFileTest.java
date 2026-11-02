package com.jobtracker.domain;

import com.jobtracker.domain.profile.ResumeFile;
import com.jobtracker.domain.user.User;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class ResumeFileTest {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());

    private static byte[] pdf(String body) {
        return ("%PDF-1.7\n" + body).getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] pdfOfSize(int size) {
        byte[] content = new byte[size];
        System.arraycopy(PDF_MAGIC, 0, content, 0, PDF_MAGIC.length);
        return content;
    }

    @Test
    void PDF_파일을_올리면_이름과_크기와_내용이_저장된다() {
        byte[] content = pdf("resume");

        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", content);

        assertThat(resume.getFileName()).isEqualTo("이력서.pdf");
        assertThat(resume.getSizeBytes()).isEqualTo(content.length);
        assertThat(resume.getContent()).isEqualTo(content);
    }

    @Test
    void 파일명에_경로가_있으면_파일명만_남긴다() {
        ResumeFile resume = ResumeFile.upload(user, "C:\\Users\\hong\\이력서.pdf", pdf("x"));

        assertThat(resume.getFileName()).isEqualTo("이력서.pdf");
    }

    @Test
    void 파일명이_공백이면_기본_이름을_사용한다() {
        ResumeFile resume = ResumeFile.upload(user, " ", pdf("x"));

        assertThat(resume.getFileName()).isEqualTo("resume.pdf");
    }

    @Test
    void 파일명이_경로로만_이루어져_있으면_기본_이름을_사용한다() {
        ResumeFile resume = ResumeFile.upload(user, "folder/", pdf("x"));

        assertThat(resume.getFileName()).isEqualTo("resume.pdf");
    }

    @Test
    void PDF가_아닌_파일은_올릴_수_없다() {
        byte[] notPdf = "plain text".getBytes(StandardCharsets.US_ASCII);

        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", notPdf), ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void PDF_시그니처보다_짧은_파일은_올릴_수_없다() {
        byte[] truncated = "%PDF".getBytes(StandardCharsets.US_ASCII);

        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", truncated), ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void PDF_시그니처만_있는_파일도_올릴_수_있다() {
        ResumeFile resume = ResumeFile.upload(user, "a.pdf", PDF_MAGIC);

        assertThat(resume.getSizeBytes()).isEqualTo(PDF_MAGIC.length);
    }

    @Test
    void 빈_파일은_올릴_수_없다() {
        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", new byte[0]), ErrorCode.EMPTY_FILE);
    }

    @Test
    void 내용이_없으면_올릴_수_없다() {
        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", null), ErrorCode.EMPTY_FILE);
    }

    @Test
    void 크기가_정확히_5MB인_파일은_올릴_수_있다() {
        ResumeFile resume = ResumeFile.upload(user, "a.pdf", pdfOfSize(ResumeFile.MAX_SIZE_BYTES));

        assertThat(resume.getSizeBytes()).isEqualTo(ResumeFile.MAX_SIZE_BYTES);
    }

    @Test
    void 크기가_5MB를_초과하면_올릴_수_없다() {
        byte[] tooLarge = pdfOfSize(ResumeFile.MAX_SIZE_BYTES + 1);

        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", tooLarge), ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void 새_파일로_교체하면_이름과_크기와_내용이_바뀐다() {
        ResumeFile resume = ResumeFile.upload(user, "old.pdf", pdf("old"));
        byte[] newContent = pdf("new content");

        resume.replace("new.pdf", newContent);

        assertThat(resume.getFileName()).isEqualTo("new.pdf");
        assertThat(resume.getSizeBytes()).isEqualTo(newContent.length);
        assertThat(resume.getContent()).isEqualTo(newContent);
    }

    @Test
    void 교체할_파일이_올바르지_않으면_기존_파일을_유지한다() {
        byte[] original = pdf("old");
        ResumeFile resume = ResumeFile.upload(user, "old.pdf", original);

        assertErrorCode(() -> resume.replace("bad.pdf", "nope".getBytes(StandardCharsets.US_ASCII)),
                ErrorCode.INVALID_FILE_TYPE);

        assertThat(resume.getFileName()).isEqualTo("old.pdf");
        assertThat(resume.getContent()).isEqualTo(original);
    }
}

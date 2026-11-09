package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.profile.ResumeFile;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class ResumeFileTest {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final FakeFileStorage storage = new FakeFileStorage();
    private final User user = registeredUser();

    private static User registeredUser() {
        User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    private static byte[] pdf(String body) {
        return ("%PDF-1.7\n" + body).getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] pdfOfSize(int size) {
        byte[] content = new byte[size];
        System.arraycopy(PDF_MAGIC, 0, content, 0, PDF_MAGIC.length);
        return content;
    }

    @Test
    void PDF_파일을_올리면_이름과_크기가_저장되고_내용은_저장소에_올라간다() {
        byte[] content = pdf("resume");

        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", content, storage);

        assertThat(resume.getFileName()).isEqualTo("이력서.pdf");
        assertThat(resume.getSizeBytes()).isEqualTo(content.length);
        assertThat(resume.getContentType()).isEqualTo("application/pdf");
        assertThat(storage.contentOf(resume.getObjectKey())).isEqualTo(content);
    }

    @Test
    void 올린_파일의_키는_사용자별_경로와_pdf_확장자를_가진다() {
        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", pdf("resume"), storage);

        assertThat(resume.getObjectKey()).startsWith("resumes/7/").endsWith(".pdf");
    }

    @Test
    void 파일명에_경로가_있으면_파일명만_남긴다() {
        ResumeFile resume = ResumeFile.upload(user, "C:\\Users\\hong\\이력서.pdf", pdf("x"), storage);

        assertThat(resume.getFileName()).isEqualTo("이력서.pdf");
    }

    @Test
    void 파일명이_공백이면_기본_이름을_사용한다() {
        ResumeFile resume = ResumeFile.upload(user, " ", pdf("x"), storage);

        assertThat(resume.getFileName()).isEqualTo("resume.pdf");
    }

    @Test
    void 파일명이_경로로만_이루어져_있으면_기본_이름을_사용한다() {
        ResumeFile resume = ResumeFile.upload(user, "folder/", pdf("x"), storage);

        assertThat(resume.getFileName()).isEqualTo("resume.pdf");
    }

    @Test
    void PDF가_아닌_파일은_올릴_수_없고_저장소에도_올리지_않는다() {
        byte[] notPdf = "plain text".getBytes(StandardCharsets.US_ASCII);

        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", notPdf, storage), ErrorCode.INVALID_FILE_TYPE);

        assertThat(storage.size()).isZero();
    }

    @Test
    void PDF_시그니처보다_짧은_파일은_올릴_수_없다() {
        byte[] truncated = "%PDF".getBytes(StandardCharsets.US_ASCII);

        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", truncated, storage), ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void PDF_시그니처만_있는_파일도_올릴_수_있다() {
        ResumeFile resume = ResumeFile.upload(user, "a.pdf", PDF_MAGIC, storage);

        assertThat(resume.getSizeBytes()).isEqualTo(PDF_MAGIC.length);
    }

    @Test
    void 빈_파일은_올릴_수_없다() {
        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", new byte[0], storage), ErrorCode.EMPTY_FILE);
    }

    @Test
    void 내용이_없으면_올릴_수_없다() {
        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", null, storage), ErrorCode.EMPTY_FILE);
    }

    @Test
    void 크기가_정확히_5MB인_파일은_올릴_수_있다() {
        ResumeFile resume = ResumeFile.upload(user, "a.pdf", pdfOfSize(ResumeFile.MAX_SIZE_BYTES), storage);

        assertThat(resume.getSizeBytes()).isEqualTo(ResumeFile.MAX_SIZE_BYTES);
    }

    @Test
    void 크기가_5MB를_초과하면_올릴_수_없다() {
        byte[] tooLarge = pdfOfSize(ResumeFile.MAX_SIZE_BYTES + 1);

        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", tooLarge, storage), ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void 저장소에_올리지_못하면_이력서를_만들_수_없다() {
        storage.failOnPut();

        assertErrorCode(() -> ResumeFile.upload(user, "a.pdf", pdf("x"), storage), ErrorCode.STORAGE_FAILED);
    }

    @Test
    void 새_파일로_교체하면_이름과_크기와_키가_바뀌고_새_내용이_저장소에_올라간다() {
        ResumeFile resume = ResumeFile.upload(user, "old.pdf", pdf("old"), storage);
        String oldKey = resume.getObjectKey();
        byte[] newContent = pdf("new content");

        resume.replace("new.pdf", newContent, storage);

        assertThat(resume.getFileName()).isEqualTo("new.pdf");
        assertThat(resume.getSizeBytes()).isEqualTo(newContent.length);
        assertThat(resume.getObjectKey()).isNotEqualTo(oldKey);
        assertThat(storage.contentOf(resume.getObjectKey())).isEqualTo(newContent);
    }

    @Test
    void 교체하면_이전_객체의_키를_돌려주고_이전_객체는_지우지_않는다() {
        ResumeFile resume = ResumeFile.upload(user, "old.pdf", pdf("old"), storage);
        String oldKey = resume.getObjectKey();

        Optional<String> previousKey = resume.replace("new.pdf", pdf("new"), storage);

        assertThat(previousKey).contains(oldKey);
        assertThat(storage.contains(oldKey)).isTrue();
    }

    @Test
    void 교체할_파일이_올바르지_않으면_기존_파일을_유지한다() {
        byte[] original = pdf("old");
        ResumeFile resume = ResumeFile.upload(user, "old.pdf", original, storage);
        String oldKey = resume.getObjectKey();

        assertErrorCode(() -> resume.replace("bad.pdf", "nope".getBytes(StandardCharsets.US_ASCII), storage),
                ErrorCode.INVALID_FILE_TYPE);

        assertThat(resume.getFileName()).isEqualTo("old.pdf");
        assertThat(resume.getObjectKey()).isEqualTo(oldKey);
        assertThat(storage.size()).isEqualTo(1);
    }

    @Test
    void 저장소에_올리지_못하면_기존_파일을_유지한다() {
        ResumeFile resume = ResumeFile.upload(user, "old.pdf", pdf("old"), storage);
        String oldKey = resume.getObjectKey();
        storage.failOnPut();

        assertErrorCode(() -> resume.replace("new.pdf", pdf("new"), storage), ErrorCode.STORAGE_FAILED);

        assertThat(resume.getFileName()).isEqualTo("old.pdf");
        assertThat(resume.getObjectKey()).isEqualTo(oldKey);
    }

    @Test
    void 올린_이력서는_저장소의_내려받기_주소를_만든다() {
        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", pdf("resume"), storage);

        PresignedUrl link = resume.downloadUrl(storage);

        assertThat(link.url()).contains(resume.getObjectKey()).contains("이력서.pdf");
        assertThat(link.expiresAt()).isEqualTo(FakeFileStorage.EXPIRES_AT);
    }

    @Test
    void 저장소에서_지우면_해당_객체가_사라진다() {
        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", pdf("resume"), storage);

        resume.deleteFrom(storage);

        assertThat(storage.contains(resume.getObjectKey())).isFalse();
    }
}

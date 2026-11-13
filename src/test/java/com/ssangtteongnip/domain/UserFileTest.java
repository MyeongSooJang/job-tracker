package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.profile.UserFile;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class UserFileTest {

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

    private UserFile upload(FileType type, String fileName, byte[] content) {
        return UserFile.upload(user, type, fileName, content, storage);
    }

    @Test
    void PDF_파일을_올리면_종류와_이름과_크기가_저장되고_내용은_저장소에_올라간다() {
        byte[] content = pdf("portfolio");

        UserFile file = upload(FileType.PORTFOLIO, "포트폴리오.pdf", content);

        assertThat(file.getType()).isEqualTo(FileType.PORTFOLIO);
        assertThat(file.getFileName()).isEqualTo("포트폴리오.pdf");
        assertThat(file.getSizeBytes()).isEqualTo(content.length);
        assertThat(file.getContentType()).isEqualTo("application/pdf");
        assertThat(storage.contentOf(file.getObjectKey())).isEqualTo(content);
    }

    @Test
    void 올린_파일의_키는_사용자별_경로와_pdf_확장자를_가진다() {
        UserFile file = upload(FileType.RESUME, "이력서.pdf", pdf("resume"));

        assertThat(file.getObjectKey()).startsWith("files/7/").endsWith(".pdf");
    }

    @Test
    void 같은_내용을_두_번_올리면_서로_다른_키로_둘_다_남는다() {
        UserFile first = upload(FileType.RESUME, "이력서.pdf", pdf("same"));
        UserFile second = upload(FileType.RESUME, "이력서.pdf", pdf("same"));

        assertThat(second.getObjectKey()).isNotEqualTo(first.getObjectKey());
        assertThat(storage.size()).isEqualTo(2);
    }

    @Test
    void 파일명에_경로가_있으면_파일명만_남긴다() {
        UserFile file = upload(FileType.RESUME, "C:\\Users\\hong\\이력서.pdf", pdf("x"));

        assertThat(file.getFileName()).isEqualTo("이력서.pdf");
    }

    @Test
    void 파일명이_공백이면_기본_이름을_사용한다() {
        UserFile file = upload(FileType.ETC, " ", pdf("x"));

        assertThat(file.getFileName()).isEqualTo("file.pdf");
    }

    @Test
    void 파일명이_경로로만_이루어져_있으면_기본_이름을_사용한다() {
        UserFile file = upload(FileType.ETC, "folder/", pdf("x"));

        assertThat(file.getFileName()).isEqualTo("file.pdf");
    }

    @Test
    void PDF가_아닌_파일은_올릴_수_없고_저장소에도_올리지_않는다() {
        byte[] notPdf = "plain text".getBytes(StandardCharsets.US_ASCII);

        assertErrorCode(() -> upload(FileType.RESUME, "a.pdf", notPdf), ErrorCode.INVALID_FILE_TYPE);

        assertThat(storage.size()).isZero();
    }

    @Test
    void PDF_시그니처보다_짧은_파일은_올릴_수_없다() {
        byte[] truncated = "%PDF".getBytes(StandardCharsets.US_ASCII);

        assertErrorCode(() -> upload(FileType.RESUME, "a.pdf", truncated), ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    void PDF_시그니처만_있는_파일도_올릴_수_있다() {
        UserFile file = upload(FileType.RESUME, "a.pdf", PDF_MAGIC);

        assertThat(file.getSizeBytes()).isEqualTo(PDF_MAGIC.length);
    }

    @Test
    void 빈_파일은_올릴_수_없다() {
        assertErrorCode(() -> upload(FileType.RESUME, "a.pdf", new byte[0]), ErrorCode.EMPTY_FILE);
    }

    @Test
    void 내용이_없으면_올릴_수_없다() {
        assertErrorCode(() -> upload(FileType.RESUME, "a.pdf", null), ErrorCode.EMPTY_FILE);
    }

    @Test
    void 크기가_정확히_5MB인_파일은_올릴_수_있다() {
        UserFile file = upload(FileType.RESUME, "a.pdf", pdfOfSize(UserFile.MAX_SIZE_BYTES));

        assertThat(file.getSizeBytes()).isEqualTo(UserFile.MAX_SIZE_BYTES);
    }

    @Test
    void 크기가_5MB를_초과하면_올릴_수_없다() {
        byte[] tooLarge = pdfOfSize(UserFile.MAX_SIZE_BYTES + 1);

        assertErrorCode(() -> upload(FileType.RESUME, "a.pdf", tooLarge), ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void 저장소에_올리지_못하면_파일을_만들_수_없다() {
        storage.failOnPut();

        assertErrorCode(() -> upload(FileType.RESUME, "a.pdf", pdf("x")), ErrorCode.STORAGE_FAILED);
    }

    @Test
    void 한도_미만이면_더_올릴_수_있다() {
        UserFile.ensureCanAdd(UserFile.MAX_FILES_PER_USER - 1);
    }

    @Test
    void 한도에_도달하면_더_올릴_수_없다() {
        assertErrorCode(() -> UserFile.ensureCanAdd(UserFile.MAX_FILES_PER_USER), ErrorCode.FILE_LIMIT_EXCEEDED);
    }

    @Test
    void 한도를_넘긴_상태에서도_올릴_수_없다() {
        assertErrorCode(() -> UserFile.ensureCanAdd(UserFile.MAX_FILES_PER_USER + 1), ErrorCode.FILE_LIMIT_EXCEEDED);
    }

    @Test
    void 올린_파일은_저장소의_내려받기_주소를_만든다() {
        UserFile file = upload(FileType.CERTIFICATE, "정보처리기사.pdf", pdf("cert"));

        PresignedUrl link = file.downloadUrl(storage);

        assertThat(link.url()).contains(file.getObjectKey()).contains("정보처리기사.pdf");
        assertThat(link.expiresAt()).isEqualTo(FakeFileStorage.EXPIRES_AT);
    }

    @Test
    void 저장소에서_지우면_해당_객체가_사라진다() {
        UserFile file = upload(FileType.RESUME, "이력서.pdf", pdf("resume"));

        file.deleteFrom(storage);

        assertThat(storage.contains(file.getObjectKey())).isFalse();
    }
}

package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.image.StudyImage;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class StudyImageTest {

    private static final long USER_ID = 7L;
    private static final byte[] PNG = withSignature(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, 0);
    private static final byte[] JPEG = withSignature(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 0);
    private static final byte[] GIF = withSignature("GIF89a".getBytes(StandardCharsets.US_ASCII), 0);

    private final FakeFileStorage storage = new FakeFileStorage();

    // 앞부분에 시그니처를 두고 뒤는 임의의 바이트로 채운 이미지 모양의 내용을 만든다.
    private static byte[] withSignature(byte[] signature, int offset) {
        byte[] content = new byte[64];
        System.arraycopy(signature, 0, content, offset, signature.length);
        return content;
    }

    private static byte[] webp() {
        byte[] content = withSignature("RIFF".getBytes(StandardCharsets.US_ASCII), 0);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, content, 8, 4);
        return content;
    }

    static Stream<Arguments> 허용하는_이미지() {
        return Stream.of(
                Arguments.of("PNG", PNG, "image/png", ".png"),
                Arguments.of("JPEG", JPEG, "image/jpeg", ".jpg"),
                Arguments.of("GIF", GIF, "image/gif", ".gif"),
                Arguments.of("WEBP", webp(), "image/webp", ".webp"));
    }

    @ParameterizedTest(name = "{0} 이미지는 형식에 맞는 콘텐츠 타입과 확장자로 저장소에 올라간다")
    @MethodSource("허용하는_이미지")
    void 허용하는_형식의_이미지는_내용_그대로_올라간다(String name, byte[] content, String contentType, String extension) {
        StudyImage image = StudyImage.upload(USER_ID, content, storage);

        assertThat(image.getContentType()).isEqualTo(contentType);
        assertThat(image.getObjectKey()).startsWith("study-images/7/").endsWith(extension);
        assertThat(image.getSizeBytes()).isEqualTo(content.length);
        assertThat(storage.contentOf(image.getObjectKey())).isEqualTo(content);
    }

    @Test
    void 저장소의_객체_키에는_추측할_수_없는_공개_아이디가_들어간다() {
        StudyImage image = StudyImage.upload(USER_ID, PNG, storage);

        assertThat(image.getPublicId()).isNotNull();
        assertThat(image.getObjectKey()).contains(image.getPublicId().toString());
    }

    @Test
    void 같은_내용을_두_번_올려도_서로_다른_공개_아이디를_가진다() {
        StudyImage first = StudyImage.upload(USER_ID, PNG, storage);
        StudyImage second = StudyImage.upload(USER_ID, PNG, storage);

        assertThat(first.getPublicId()).isNotEqualTo(second.getPublicId());
        assertThat(storage.size()).isEqualTo(2);
    }

    @Test
    void 본문에_넣는_주소는_도메인_없이_공개_아이디_경로만_가진다() {
        StudyImage image = StudyImage.upload(USER_ID, PNG, storage);

        assertThat(image.publicPath()).isEqualTo("/api/images/" + image.getPublicId());
    }

    @Test
    void 보여_줄_주소는_저장소가_만든_inline_주소를_돌려준다() {
        StudyImage image = StudyImage.upload(USER_ID, PNG, storage);

        PresignedUrl link = image.viewUrl(storage);

        assertThat(link.url()).isEqualTo("https://fake-storage/" + image.getObjectKey());
    }

    @Test
    void 이미지가_아닌_파일은_올릴_수_없고_저장소에도_남지_않는다() {
        byte[] pdf = "%PDF-1.7 body".getBytes(StandardCharsets.US_ASCII);

        assertErrorCode(() -> StudyImage.upload(USER_ID, pdf, storage), ErrorCode.INVALID_IMAGE_TYPE);
        assertThat(storage.size()).isZero();
    }

    @Test
    void 확장자가_이미지여도_내용이_이미지가_아니면_올릴_수_없다() {
        byte[] script = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        assertErrorCode(() -> StudyImage.upload(USER_ID, script, storage), ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    void RIFF로_시작해도_WEBP가_아니면_올릴_수_없다() {
        byte[] wav = withSignature("RIFF".getBytes(StandardCharsets.US_ASCII), 0);
        System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, wav, 8, 4);

        assertErrorCode(() -> StudyImage.upload(USER_ID, wav, storage), ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    void 시그니처보다_짧은_내용은_올릴_수_없다() {
        byte[] truncated = {(byte) 0x89, 'P', 'N'};

        assertErrorCode(() -> StudyImage.upload(USER_ID, truncated, storage), ErrorCode.INVALID_IMAGE_TYPE);
    }

    @Test
    void 비어_있으면_올릴_수_없다() {
        assertErrorCode(() -> StudyImage.upload(USER_ID, new byte[0], storage), ErrorCode.EMPTY_FILE);
        assertErrorCode(() -> StudyImage.upload(USER_ID, null, storage), ErrorCode.EMPTY_FILE);
    }

    @Test
    void 최대_크기까지는_올릴_수_있고_넘으면_올릴_수_없다() {
        byte[] atLimit = new byte[StudyImage.MAX_SIZE_BYTES];
        System.arraycopy(PNG, 0, atLimit, 0, 8);
        byte[] overLimit = new byte[StudyImage.MAX_SIZE_BYTES + 1];
        System.arraycopy(PNG, 0, overLimit, 0, 8);

        assertThat(StudyImage.upload(USER_ID, atLimit, storage).getSizeBytes()).isEqualTo(StudyImage.MAX_SIZE_BYTES);
        assertErrorCode(() -> StudyImage.upload(USER_ID, overLimit, storage), ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void 저장소가_실패하면_예외가_전파된다() {
        storage.failOnPut();

        assertErrorCode(() -> StudyImage.upload(USER_ID, PNG, storage), ErrorCode.STORAGE_FAILED);
    }
}

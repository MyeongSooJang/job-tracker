package com.ssangtteongnip.domain.image;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

// 업로드를 허용하는 이미지 형식. 확장자나 Content-Type은 클라이언트가 속일 수 있으므로 파일 앞부분(매직 바이트)으로 판별한다.
enum ImageFormat {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp");

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87_SIGNATURE = "GIF87a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] GIF89_SIGNATURE = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MARK = "WEBP".getBytes(StandardCharsets.US_ASCII);
    private static final int WEBP_MARK_OFFSET = 8;

    private final String contentType;
    private final String extension;

    ImageFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    String contentType() {
        return contentType;
    }

    String extension() {
        return extension;
    }

    static Optional<ImageFormat> detect(byte[] content) {
        return Arrays.stream(values()).filter(format -> format.matches(content)).findFirst();
    }

    private boolean matches(byte[] content) {
        return switch (this) {
            case PNG -> startsWith(content, 0, PNG_SIGNATURE);
            case JPEG -> startsWith(content, 0, JPEG_SIGNATURE);
            case GIF -> startsWith(content, 0, GIF87_SIGNATURE) || startsWith(content, 0, GIF89_SIGNATURE);
            case WEBP -> startsWith(content, 0, RIFF) && startsWith(content, WEBP_MARK_OFFSET, WEBP_MARK);
        };
    }

    private static boolean startsWith(byte[] content, int offset, byte[] signature) {
        return content.length >= offset + signature.length
                && Arrays.equals(content, offset, offset + signature.length, signature, 0, signature.length);
    }
}

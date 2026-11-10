package com.ssangtteongnip.domain.image;

import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 학습 기록 본문에 넣는 이미지. 본문에는 객체 키가 아니라 추측할 수 없는 publicId 경로만 남기고,
// 보여줄 때마다 서버가 짧게 유효한 presigned URL로 안내한다. 그래서 버킷은 비공개로 두면서도 본문의 주소는 만료되지 않는다.
@Entity
@Table(name = "study_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StudyImage {

    public static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String PUBLIC_PATH_PREFIX = "/api/images/";
    private static final Pattern REFERENCE = Pattern.compile(Pattern.quote(PUBLIC_PATH_PREFIX)
            + "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 검증을 통과한 뒤에만 저장소에 올린다. 검증이나 업로드가 실패하면 객체가 만들어지지 않는다.
    public static StudyImage upload(Long userId, byte[] content, FileStorage storage) {
        ImageFormat format = validate(content);
        StudyImage image = new StudyImage();
        image.publicId = UUID.randomUUID();
        image.userId = userId;
        image.objectKey = "study-images/" + userId + "/" + image.publicId + "." + format.extension();
        image.contentType = format.contentType();
        image.sizeBytes = content.length;
        storage.put(image.objectKey, content, image.contentType);
        return image;
    }

    // 본문(마크다운)이 가리키는 이미지의 공개 아이디들. 본문에 주소를 적는 형식(publicPath)을 한곳에서만 안다.
    public static Set<UUID> referencedIn(String markdown) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (markdown == null) {
            return ids;
        }
        Matcher matcher = REFERENCE.matcher(markdown);
        while (matcher.find()) {
            ids.add(UUID.fromString(matcher.group(1)));
        }
        return ids;
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public void deleteFrom(FileStorage storage) {
        storage.delete(objectKey);
    }

    public PresignedUrl viewUrl(FileStorage storage) {
        return storage.presignedViewUrl(objectKey);
    }

    // 본문(마크다운)에 저장하는 주소. 도메인이 바뀌어도 깨지지 않도록 경로만 둔다.
    public String publicPath() {
        return PUBLIC_PATH_PREFIX + publicId;
    }

    private static ImageFormat validate(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        return ImageFormat.detect(content)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_IMAGE_TYPE));
    }
}

package com.ssangtteongnip.domain.profile;

import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Entity
@Table(name = "user_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserFile {

    public static final int MAX_FILES_PER_USER = 20;
    public static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String DEFAULT_FILE_NAME = "file.pdf";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType type;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "content_type")
    private String contentType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 검증과 저장소 업로드가 끝난 뒤에만 객체를 돌려준다. 실패하면 파일은 만들어지지 않는다.
    public static UserFile upload(User user, FileType type, String fileName, byte[] content, FileStorage storage) {
        validate(content);
        UserFile file = new UserFile();
        file.user = user;
        file.type = type;
        file.objectKey = newObjectKey(user);
        storage.put(file.objectKey, content, CONTENT_TYPE);
        file.fileName = sanitize(fileName);
        file.sizeBytes = content.length;
        file.contentType = CONTENT_TYPE;
        return file;
    }

    // 현재 개수가 한도에 이미 닿았으면 더 올릴 수 없다.
    public static void ensureCanAdd(long currentCount) {
        if (currentCount >= MAX_FILES_PER_USER) {
            throw new BusinessException(ErrorCode.FILE_LIMIT_EXCEEDED);
        }
    }

    public PresignedUrl downloadUrl(FileStorage storage) {
        return storage.presignedDownloadUrl(objectKey, fileName);
    }

    public void deleteFrom(FileStorage storage) {
        storage.delete(objectKey);
    }

    private static String newObjectKey(User user) {
        return "files/" + user.getId() + "/" + UUID.randomUUID() + ".pdf";
    }

    private static void validate(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        if (content.length < PDF_MAGIC.length
                || !Arrays.equals(content, 0, PDF_MAGIC.length, PDF_MAGIC, 0, PDF_MAGIC.length)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return DEFAULT_FILE_NAME;
        }
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.isEmpty() ? DEFAULT_FILE_NAME : name;
    }
}

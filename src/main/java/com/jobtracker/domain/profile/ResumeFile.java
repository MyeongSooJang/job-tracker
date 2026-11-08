package com.jobtracker.domain.profile;

import com.jobtracker.domain.user.User;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "resume_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ResumeFile {

    public static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String DEFAULT_FILE_NAME = "resume.pdf";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "content_type")
    private String contentType;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static ResumeFile upload(User user, String fileName, byte[] content, FileStorage storage) {
        ResumeFile resume = new ResumeFile();
        resume.user = user;
        resume.replace(fileName, content, storage);
        return resume;
    }

    // 새 객체를 새 키로 올린 뒤 필드를 바꾼다. 검증이나 업로드가 실패하면 기존 상태를 유지한다.
    // 이전 객체는 지우지 않고 키만 돌려준다. DB 커밋 뒤에 호출한 쪽이 지운다.
    public Optional<String> replace(String fileName, byte[] content, FileStorage storage) {
        validate(content);
        String newKey = newObjectKey();
        storage.put(newKey, content, CONTENT_TYPE);

        Optional<String> previousKey = Optional.ofNullable(this.objectKey);
        this.fileName = sanitize(fileName);
        this.sizeBytes = content.length;
        this.objectKey = newKey;
        this.contentType = CONTENT_TYPE;
        return previousKey;
    }

    public PresignedUrl downloadUrl(FileStorage storage) {
        return storage.presignedDownloadUrl(objectKey, fileName);
    }

    public void deleteFrom(FileStorage storage) {
        storage.delete(objectKey);
    }

    private String newObjectKey() {
        return "resumes/" + user.getId() + "/" + UUID.randomUUID() + ".pdf";
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

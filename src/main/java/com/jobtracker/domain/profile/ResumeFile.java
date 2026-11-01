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

@Entity
@Table(name = "resume_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ResumeFile {

    public static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String DEFAULT_FILE_NAME = "resume.pdf";
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

    @Column(nullable = false, length = MAX_SIZE_BYTES)
    private byte[] content;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static ResumeFile upload(User user, String fileName, byte[] content) {
        ResumeFile resume = new ResumeFile();
        resume.user = user;
        resume.replace(fileName, content);
        return resume;
    }

    public void replace(String fileName, byte[] content) {
        validate(content);
        this.fileName = sanitize(fileName);
        this.sizeBytes = content.length;
        this.content = content;
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

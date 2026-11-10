package com.ssangtteongnip.domain.studylog;

import com.ssangtteongnip.domain.image.StudyImage;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "study_logs",
        indexes = @Index(name = "idx_study_logs_user_date", columnList = "user_id, study_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StudyLog {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MIN_MINUTES = 0;
    private static final int MAX_MINUTES = 24 * 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate studyDate;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int studyMinutes;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static StudyLog create(Long userId, LocalDate studyDate, String title, String content,
                                  int studyMinutes, LocalDate today) {
        if (studyDate.isAfter(today)) {
            throw new BusinessException(ErrorCode.FUTURE_STUDY_DATE);
        }
        StudyLog studyLog = new StudyLog();
        studyLog.userId = userId;
        studyLog.studyDate = studyDate;
        studyLog.write(title, content, studyMinutes);
        return studyLog;
    }

    public void write(String title, String content, int studyMinutes) {
        if (title == null || title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.STUDY_TITLE_REQUIRED);
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.STUDY_CONTENT_REQUIRED);
        }
        if (studyMinutes < MIN_MINUTES || studyMinutes > MAX_MINUTES) {
            throw new BusinessException(ErrorCode.INVALID_STUDY_MINUTES);
        }
        this.title = title;
        this.content = content;
        this.studyMinutes = studyMinutes;
    }

    // 본문이 쓰는 이미지들
    public Set<UUID> imageIds() {
        return StudyImage.referencedIn(content);
    }

    // 새 본문으로 바꾸면 더는 쓰지 않게 되는 이미지들 (바꾸기 전에 물어본다)
    public Set<UUID> imageIdsRemovedBy(String newContent) {
        Set<UUID> removed = new LinkedHashSet<>(imageIds());
        removed.removeAll(StudyImage.referencedIn(newContent));
        return removed;
    }

    public boolean isOn(LocalDate date) {
        return studyDate.equals(date);
    }

    public int minutesOn(LocalDate date) {
        return isOn(date) ? studyMinutes : 0;
    }
}

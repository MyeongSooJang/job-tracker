package com.jobtracker.domain.studylog;

import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_logs",
        uniqueConstraints = @UniqueConstraint(name = "uk_study_logs_user_date", columnNames = {"user_id", "study_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StudyLog {

    private static final int MIN_MINUTES = 0;
    private static final int MAX_MINUTES = 24 * 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate studyDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int studyMinutes;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static StudyLog create(Long userId, LocalDate studyDate, String content, int studyMinutes, LocalDate today) {
        if (studyDate.isAfter(today)) {
            throw new BusinessException(ErrorCode.FUTURE_STUDY_DATE);
        }
        StudyLog studyLog = new StudyLog();
        studyLog.userId = userId;
        studyLog.studyDate = studyDate;
        studyLog.write(content, studyMinutes);
        return studyLog;
    }

    public void write(String content, int studyMinutes) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.STUDY_CONTENT_REQUIRED);
        }
        if (studyMinutes < MIN_MINUTES || studyMinutes > MAX_MINUTES) {
            throw new BusinessException(ErrorCode.INVALID_STUDY_MINUTES);
        }
        this.content = content;
        this.studyMinutes = studyMinutes;
    }

    public int minutesOn(LocalDate date) {
        return studyDate.equals(date) ? studyMinutes : 0;
    }
}

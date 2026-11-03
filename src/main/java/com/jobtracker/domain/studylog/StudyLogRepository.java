package com.jobtracker.domain.studylog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyLogRepository {
    StudyLog save(StudyLog studyLog);
    void delete(StudyLog studyLog);
    Optional<StudyLog> findByUserIdAndStudyDate(Long userId, LocalDate studyDate);
    List<StudyLog> findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(Long userId, LocalDate from, LocalDate to);
    List<StudyLog> findTop10ByUserIdOrderByStudyDateDesc(Long userId);
    long countByUserIdAndStudyDateBetween(Long userId, LocalDate from, LocalDate to);

    // 기록한 날짜만 최신순 (연속 작성일 계산용, 본문은 읽지 않는다)
    List<LocalDate> findStudyDatesByUserId(Long userId);
}

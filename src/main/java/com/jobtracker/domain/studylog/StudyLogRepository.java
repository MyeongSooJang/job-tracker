package com.jobtracker.domain.studylog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyLogRepository {
    StudyLog save(StudyLog studyLog);
    void delete(StudyLog studyLog);

    // 남의 글은 id를 알아도 찾을 수 없도록 사용자까지 함께 조회한다.
    Optional<StudyLog> findByIdAndUserId(Long id, Long userId);

    // 최신 날짜부터, 같은 날은 나중에 쓴 글이 먼저
    List<StudyLog> findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(Long userId, LocalDate from, LocalDate to);

    List<StudyLog> findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(Long userId, LocalDate from, LocalDate to);
    long countByUserIdAndStudyDateBetween(Long userId, LocalDate from, LocalDate to);

    // 기록한 날짜만 중복 없이 최신순 (연속 작성일 계산용, 본문은 읽지 않는다)
    List<LocalDate> findStudyDatesByUserId(Long userId);
}

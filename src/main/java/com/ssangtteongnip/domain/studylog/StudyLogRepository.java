package com.ssangtteongnip.domain.studylog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyLogRepository {
    StudyLog save(StudyLog studyLog);
    void delete(StudyLog studyLog);

    // 남의 글은 id를 알아도 찾을 수 없도록 사용자까지 함께 조회한다.
    Optional<StudyLog> findByIdAndUserId(Long id, Long userId);

    // 최신 날짜부터, 같은 날은 나중에 쓴 글이 먼저
    List<StudyLog> findInPeriod(Long userId, LocalDate from, LocalDate to);

    // 어느 사용자의 글이든 본문에 이 문자열이 들어 있는지 (이미지 주소를 다른 글이 아직 쓰는지 확인할 때 쓴다)
    boolean existsByContentContaining(String text);

    // 글이 있는 날짜 수 (같은 날 여러 글은 하루로 센다)
    long countStudyDatesBetween(Long userId, LocalDate from, LocalDate to);

    // 기록한 날짜만 중복 없이 최신순 (연속 작성일 계산용, 본문은 읽지 않는다)
    List<LocalDate> findStudyDatesByUserId(Long userId);
}

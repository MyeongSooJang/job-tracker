package com.jobtracker.application.service;

import com.jobtracker.application.service.dto.StudyLogRequest;
import com.jobtracker.application.service.dto.StudyLogResponse;
import com.jobtracker.application.service.dto.StudyStatsResponse;
import com.jobtracker.domain.studylog.StudyLog;
import com.jobtracker.domain.studylog.StudyLogRepository;
import com.jobtracker.domain.studylog.StudyLogs;
import com.jobtracker.domain.studylog.StudyStreak;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyLogService {

    private static final int CHART_DAYS = 7;

    private final StudyLogRepository studyLogRepository;
    private final Clock clock;

    @Transactional
    public StudyLogResponse save(Long userId, LocalDate date, StudyLogRequest request) {
        Optional<StudyLog> existing = studyLogRepository.findByUserIdAndStudyDate(userId, date);
        if (existing.isPresent()) {
            StudyLog studyLog = existing.get();
            studyLog.write(request.content(), request.minutesOrZero());
            return StudyLogResponse.from(studyLog);
        }
        StudyLog created = StudyLog.create(userId, date, request.content(), request.minutesOrZero(), today());
        return StudyLogResponse.from(studyLogRepository.save(created));
    }

    public StudyLogResponse find(Long userId, LocalDate date) {
        return StudyLogResponse.from(findOrThrow(userId, date));
    }

    public List<StudyLogResponse> findByMonth(Long userId, YearMonth month) {
        return studyLogRepository
                .findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(userId, month.atDay(1), month.atEndOfMonth())
                .stream().map(StudyLogResponse::from).toList();
    }

    public List<StudyLogResponse> findRecent(Long userId) {
        return studyLogRepository.findTop10ByUserIdOrderByStudyDateDesc(userId)
                .stream().map(StudyLogResponse::from).toList();
    }

    @Transactional
    public void delete(Long userId, LocalDate date) {
        studyLogRepository.delete(findOrThrow(userId, date));
    }

    public StudyStatsResponse stats(Long userId) {
        LocalDate today = today();
        LocalDate chartStart = today.minusDays(CHART_DAYS - 1);
        YearMonth month = YearMonth.from(today);

        StudyStreak streak = StudyStreak.of(studyLogRepository.findStudyDatesByUserId(userId));
        StudyLogs chartLogs = new StudyLogs(
                studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(userId, chartStart, today));
        long monthWrittenDays = studyLogRepository
                .countByUserIdAndStudyDateBetween(userId, month.atDay(1), month.atEndOfMonth());

        return new StudyStatsResponse(streak.current(today), (int) monthWrittenDays,
                chartLogs.minutesByDay(chartStart, today));
    }

    private StudyLog findOrThrow(Long userId, LocalDate date) {
        return studyLogRepository.findByUserIdAndStudyDate(userId, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_LOG_NOT_FOUND));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}

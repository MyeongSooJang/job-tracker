package com.jobtracker.application.service;

import com.jobtracker.application.service.dto.StudyLogCreateRequest;
import com.jobtracker.application.service.dto.StudyLogRequest;
import com.jobtracker.application.service.dto.StudyLogResponse;
import com.jobtracker.application.service.dto.StudyStatsResponse;
import com.jobtracker.domain.studylog.StudyLog;
import com.jobtracker.domain.studylog.StudyLogRepository;
import com.jobtracker.domain.studylog.StudyLogs;
import com.jobtracker.domain.studylog.StudyPeriod;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyLogService {

    private static final int CHART_DAYS = 7;
    private static final int MAX_LIST_DAYS = 62;

    private final StudyLogRepository studyLogRepository;
    private final Clock clock;

    @Transactional
    public StudyLogResponse create(Long userId, StudyLogCreateRequest request) {
        StudyLog created = StudyLog.create(userId, request.studyDate(), request.title(), request.content(),
                request.minutesOrZero(), today());
        return StudyLogResponse.from(studyLogRepository.save(created));
    }

    @Transactional
    public StudyLogResponse update(Long userId, Long id, StudyLogRequest request) {
        StudyLog studyLog = findOrThrow(userId, id);
        studyLog.write(request.title(), request.content(), request.minutesOrZero());
        return StudyLogResponse.from(studyLog);
    }

    public StudyLogResponse find(Long userId, Long id) {
        return StudyLogResponse.from(findOrThrow(userId, id));
    }

    public List<StudyLogResponse> findRange(Long userId, LocalDate from, LocalDate to) {
        StudyPeriod period = StudyPeriod.of(from, to, MAX_LIST_DAYS);
        return studyLogRepository
                .findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(userId, period.from(), period.to())
                .stream().map(StudyLogResponse::from).toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        studyLogRepository.delete(findOrThrow(userId, id));
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

    private StudyLog findOrThrow(Long userId, Long id) {
        return studyLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_LOG_NOT_FOUND));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}

package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.HeatmapDayResponse;
import com.ssangtteongnip.application.service.dto.StudyLogCreateRequest;
import com.ssangtteongnip.application.service.dto.StudyLogRequest;
import com.ssangtteongnip.application.service.dto.StudyLogResponse;
import com.ssangtteongnip.application.service.dto.StudyStatsResponse;
import com.ssangtteongnip.domain.studylog.StudyLog;
import com.ssangtteongnip.domain.studylog.StudyLogRepository;
import com.ssangtteongnip.domain.studylog.StudyLogs;
import com.ssangtteongnip.domain.studylog.StudyPeriod;
import com.ssangtteongnip.domain.studylog.StudyStreak;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyLogService {

    private static final int MAX_HEATMAP_DAYS = 400;
    private static final int MAX_LIST_DAYS = 62;

    private final StudyLogRepository studyLogRepository;
    private final StudyImageService studyImageService;
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
        Set<UUID> removedImages = studyLog.imageIdsRemovedBy(request.content());
        studyLog.write(request.title(), request.content(), request.minutesOrZero());
        studyImageService.deleteUnused(userId, removedImages);
        return StudyLogResponse.from(studyLog);
    }

    public StudyLogResponse find(Long userId, Long id) {
        return StudyLogResponse.from(findOrThrow(userId, id));
    }

    public List<StudyLogResponse> findRange(Long userId, LocalDate from, LocalDate to) {
        StudyPeriod period = StudyPeriod.of(from, to, MAX_LIST_DAYS);
        return studyLogRepository
                .findInPeriod(userId, period.from(), period.to())
                .stream().map(StudyLogResponse::from).toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        StudyLog studyLog = findOrThrow(userId, id);
        Set<UUID> images = studyLog.imageIds();
        studyLogRepository.delete(studyLog);
        studyImageService.deleteUnused(userId, images);
    }

    public List<HeatmapDayResponse> heatmap(Long userId, LocalDate from, LocalDate to) {
        StudyPeriod period = StudyPeriod.of(from, to, MAX_HEATMAP_DAYS);
        StudyLogs logs = new StudyLogs(studyLogRepository
                .findInPeriod(userId, period.from(), period.to()));
        return logs.dailySummaries().stream().map(HeatmapDayResponse::from).toList();
    }

    public StudyStatsResponse stats(Long userId) {
        LocalDate today = today();
        YearMonth month = YearMonth.from(today);

        StudyStreak streak = StudyStreak.of(studyLogRepository.findStudyDatesByUserId(userId));
        StudyLogs todayLogs = new StudyLogs(studyLogRepository
                .findInPeriod(userId, today, today));
        long monthWrittenDays = studyLogRepository
                .countStudyDatesBetween(userId, month.atDay(1), month.atEndOfMonth());

        return new StudyStatsResponse(streak.current(today), streak.longest(),
                (int) monthWrittenDays, todayLogs.minutesOn(today));
    }

    private StudyLog findOrThrow(Long userId, Long id) {
        return studyLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_LOG_NOT_FOUND));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}

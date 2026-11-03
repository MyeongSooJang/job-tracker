package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.StudyLogService;
import com.jobtracker.application.service.dto.StudyLogRequest;
import com.jobtracker.application.service.dto.StudyLogResponse;
import com.jobtracker.application.service.dto.StudyStatsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/me/study-logs")
@RequiredArgsConstructor
public class StudyLogController {

    private final StudyLogService studyLogService;

    @PutMapping("/{date}")
    public StudyLogResponse save(@AuthenticationPrincipal Long userId,
                                 @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestBody @Valid StudyLogRequest request) {
        return studyLogService.save(userId, date, request);
    }

    @GetMapping("/{date}")
    public StudyLogResponse find(@AuthenticationPrincipal Long userId,
                                 @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return studyLogService.find(userId, date);
    }

    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId,
                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        studyLogService.delete(userId, date);
    }

    @GetMapping
    public List<StudyLogResponse> findByMonth(@AuthenticationPrincipal Long userId,
                                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return studyLogService.findByMonth(userId, month);
    }

    @GetMapping("/recent")
    public List<StudyLogResponse> findRecent(@AuthenticationPrincipal Long userId) {
        return studyLogService.findRecent(userId);
    }

    @GetMapping("/stats")
    public StudyStatsResponse stats(@AuthenticationPrincipal Long userId) {
        return studyLogService.stats(userId);
    }
}

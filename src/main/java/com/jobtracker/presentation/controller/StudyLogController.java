package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.StudyLogService;
import com.jobtracker.application.service.dto.HeatmapDayResponse;
import com.jobtracker.application.service.dto.StudyLogCreateRequest;
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
import java.util.List;

@RestController
@RequestMapping("/api/me/study-logs")
@RequiredArgsConstructor
public class StudyLogController {

    private final StudyLogService studyLogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyLogResponse create(@AuthenticationPrincipal Long userId,
                                   @RequestBody @Valid StudyLogCreateRequest request) {
        return studyLogService.create(userId, request);
    }

    @PutMapping("/{id}")
    public StudyLogResponse update(@AuthenticationPrincipal Long userId,
                                   @PathVariable Long id,
                                   @RequestBody @Valid StudyLogRequest request) {
        return studyLogService.update(userId, id, request);
    }

    @GetMapping("/{id}")
    public StudyLogResponse find(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return studyLogService.find(userId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        studyLogService.delete(userId, id);
    }

    @GetMapping
    public List<StudyLogResponse> findRange(@AuthenticationPrincipal Long userId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return studyLogService.findRange(userId, from, to);
    }

    @GetMapping("/heatmap")
    public List<HeatmapDayResponse> heatmap(@AuthenticationPrincipal Long userId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return studyLogService.heatmap(userId, from, to);
    }

    @GetMapping("/stats")
    public StudyStatsResponse stats(@AuthenticationPrincipal Long userId) {
        return studyLogService.stats(userId);
    }
}

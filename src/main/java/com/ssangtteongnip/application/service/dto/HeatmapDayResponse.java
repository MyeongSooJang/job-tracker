package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.studylog.DailyStudySummary;

import java.time.LocalDate;

public record HeatmapDayResponse(LocalDate date, int count, int minutes) {

    public static HeatmapDayResponse from(DailyStudySummary summary) {
        return new HeatmapDayResponse(summary.date(), summary.count(), summary.minutes());
    }
}

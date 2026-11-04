package com.jobtracker.application.service.dto;

import com.jobtracker.domain.studylog.DailyStudySummary;

import java.time.LocalDate;

public record HeatmapDayResponse(LocalDate date, int count, int minutes) {

    public static HeatmapDayResponse from(DailyStudySummary summary) {
        return new HeatmapDayResponse(summary.date(), summary.count(), summary.minutes());
    }
}

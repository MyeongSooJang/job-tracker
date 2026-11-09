package com.ssangtteongnip.domain.studylog;

import java.time.LocalDate;

public record DailyStudySummary(LocalDate date, int count, int minutes) {
}

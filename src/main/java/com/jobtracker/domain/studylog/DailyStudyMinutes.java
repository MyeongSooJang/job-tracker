package com.jobtracker.domain.studylog;

import java.time.LocalDate;

public record DailyStudyMinutes(LocalDate date, int minutes) {
}

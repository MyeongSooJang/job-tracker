package com.jobtracker.application.service.dto;

import com.jobtracker.domain.studylog.DailyStudyMinutes;

import java.util.List;

public record StudyStatsResponse(int currentStreak, int monthWrittenDays, List<DailyStudyMinutes> last7Days) {
}

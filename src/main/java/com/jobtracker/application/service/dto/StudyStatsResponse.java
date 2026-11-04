package com.jobtracker.application.service.dto;

public record StudyStatsResponse(int currentStreak, int longestStreak, int monthWrittenDays, int todayMinutes) {
}

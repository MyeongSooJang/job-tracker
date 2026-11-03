package com.jobtracker.application.service.dto;

import jakarta.validation.constraints.Size;

public record StudyLogRequest(
        @Size(max = 10000, message = "공부 내용은 10000자 이하여야 합니다.") String content,
        Integer studyMinutes) {

    public int minutesOrZero() {
        return studyMinutes == null ? 0 : studyMinutes;
    }
}

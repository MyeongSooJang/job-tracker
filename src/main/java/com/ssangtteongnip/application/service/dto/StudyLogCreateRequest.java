package com.ssangtteongnip.application.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudyLogCreateRequest(
        @NotNull(message = "날짜를 입력해주세요.") LocalDate studyDate,
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.") String title,
        @Size(max = 10000, message = "공부 내용은 10000자 이하여야 합니다.") String content,
        Integer studyMinutes) {

    public int minutesOrZero() {
        return studyMinutes == null ? 0 : studyMinutes;
    }
}

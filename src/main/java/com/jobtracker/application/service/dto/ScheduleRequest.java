package com.jobtracker.application.service.dto;

import com.jobtracker.domain.schedule.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ScheduleRequest(
        @NotNull Long applicationId,
        @NotBlank String title,
        @NotNull LocalDateTime scheduledAt,
        @NotNull ScheduleType type,
        String note
) {}

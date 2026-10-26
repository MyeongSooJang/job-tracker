package com.jobtracker.application.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record InterviewRequest(
        @Min(1) int round,
        @NotNull LocalDateTime interviewDate,
        String location,
        String note
) {}

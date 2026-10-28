package com.jobtracker.application.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record InterviewRequest(
        @Min(1) int round,
        @NotNull LocalDateTime interviewDate,
        String location,
        String note,
        @Min(0) @Max(100) Integer score
) {}

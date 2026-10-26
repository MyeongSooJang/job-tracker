package com.jobtracker.presentation.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record InterviewRequest(
        @NotNull int round,
        @NotNull LocalDateTime interviewDate,
        String location,
        String note
) {}

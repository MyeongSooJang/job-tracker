package com.jobtracker.application.service.dto;

import jakarta.validation.constraints.NotBlank;

public record InterviewQuestionRequest(
        @NotBlank String question,
        String answer,
        String memo
) {}

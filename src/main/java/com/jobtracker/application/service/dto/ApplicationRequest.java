package com.jobtracker.application.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ApplicationRequest(
        @NotNull Long companyId,
        @NotBlank String position,
        @NotNull LocalDate appliedDate
) {}

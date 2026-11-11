package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.application.JobSite;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ApplicationRequest(
        @NotNull Long companyId,
        @NotBlank String position,
        @NotNull LocalDate appliedDate,
        @NotNull JobSite jobSite
) {}

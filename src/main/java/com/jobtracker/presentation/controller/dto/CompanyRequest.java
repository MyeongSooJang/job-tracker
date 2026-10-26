package com.jobtracker.presentation.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank String name,
        String industry,
        String website
) {}

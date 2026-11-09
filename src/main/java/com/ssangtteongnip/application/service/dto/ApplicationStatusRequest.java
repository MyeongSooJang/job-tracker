package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusRequest(@NotNull ApplicationStatus status) {}

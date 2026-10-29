package com.jobtracker.application.service.dto;

import com.jobtracker.domain.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusRequest(@NotNull ApplicationStatus status) {}

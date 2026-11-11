package com.ssangtteongnip.application.service.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ApplicationDeadlineRequest(@NotNull LocalDate deadline) {}

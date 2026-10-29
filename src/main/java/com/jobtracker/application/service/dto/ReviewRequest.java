package com.jobtracker.application.service.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(@NotBlank String content) {}

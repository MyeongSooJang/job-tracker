package com.jobtracker.application.service.dto;

import jakarta.validation.constraints.NotBlank;

public record ScheduleAddressRequest(@NotBlank String address) {}

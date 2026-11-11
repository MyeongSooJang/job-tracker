package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.application.ResumeView;
import jakarta.validation.constraints.NotNull;

public record ApplicationResumeViewRequest(@NotNull ResumeView resumeView) {}

package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.application.JobSite;
import jakarta.validation.constraints.NotNull;

public record ApplicationJobSiteRequest(@NotNull JobSite jobSite) {}

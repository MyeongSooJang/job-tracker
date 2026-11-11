package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.application.JobSite;
import com.ssangtteongnip.domain.application.ResumeView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ApplicationRequest(
        @NotNull Long companyId,
        @NotBlank String position,
        @NotNull LocalDate appliedDate,
        @NotNull JobSite jobSite,
        LocalDate deadline,
        ResumeView resumeView
) {
    // 열람 여부를 생략하면 미열람으로 등록한다.
    public ApplicationRequest {
        resumeView = resumeView == null ? ResumeView.NOT_VIEWED : resumeView;
    }
}

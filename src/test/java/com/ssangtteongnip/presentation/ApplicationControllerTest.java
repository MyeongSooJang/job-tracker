package com.ssangtteongnip.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.application.service.ApplicationService;
import com.ssangtteongnip.application.service.dto.JobSiteStatisticsResponse;
import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.application.JobSite;
import com.ssangtteongnip.domain.application.ResumeView;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.presentation.controller.ApplicationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @MockBean TokenProvider tokenProvider;
    @Autowired ObjectMapper objectMapper;
    @MockBean ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private Application sampleApplication() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return Application.create(1L, company, "백엔드 개발자", LocalDate.now());
    }

    @Test
    void POST_applications_지원_건을_등록한다() throws Exception {
        given(applicationService.register(eq(1L), any())).willReturn(sampleApplication());

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("companyId", 1, "position", "백엔드 개발자", "appliedDate", LocalDate.now().toString(),
                                        "jobSite", "OTHER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESUME_SUBMITTED"));
    }

    @Test
    void 구직_사이트가_없으면_지원_건_등록은_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("companyId", 1, "position", "백엔드 개발자", "appliedDate", LocalDate.now().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void PATCH_applications_job_site_구직_사이트를_변경한다() throws Exception {
        Application app = sampleApplication();
        app.changeJobSite(JobSite.WANTED);
        given(applicationService.changeJobSite(eq(1L), eq(1L), any())).willReturn(app);

        mockMvc.perform(patch("/api/applications/1/job-site")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jobSite", "WANTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobSite").value("WANTED"));
    }

    @Test
    void 구직_사이트가_없으면_구직_사이트_변경은_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/applications/1/job-site")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 토큰이_없으면_구직_사이트_변경은_401을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/applications/1/job-site")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jobSite", "WANTED"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void PATCH_applications_status_상태를_전이한다() throws Exception {
        Application app = sampleApplication();
        app.startInterview(java.time.LocalDateTime.of(2026, 11, 5, 10, 0));
        given(applicationService.decideResult(eq(1L), eq(1L), any())).willReturn(app);

        mockMvc.perform(patch("/api/applications/1/status")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INTERVIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    @Test
    void GET_applications_status로_면접_단계_지원_건만_조회한다() throws Exception {
        Application app = sampleApplication();
        app.startInterview(java.time.LocalDateTime.of(2026, 11, 5, 10, 0));
        given(applicationService.findByStatus(1L, ApplicationStatus.INTERVIEW)).willReturn(java.util.List.of(app));

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", BEARER).param("status", "INTERVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INTERVIEW"));
    }

    @Test
    void 지원_건_등록_시_필수값이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("position", "백엔드 개발자"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 상태_전이_요청에_상태가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/applications/1/status")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 허용되지_않는_상태_전이는_400과_에러_코드를_반환한다() throws Exception {
        given(applicationService.decideResult(eq(1L), eq(1L), any()))
                .willThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(patch("/api/applications/1/status")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACCEPTED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void 단건_조회한_지원_건의_상태와_직무를_반환한다() throws Exception {
        given(applicationService.findById(1L, 1L)).willReturn(sampleApplication());

        mockMvc.perform(get("/api/applications/1").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("백엔드 개발자"))
                .andExpect(jsonPath("$.status").value("RESUME_SUBMITTED"));
    }

    @Test
    void GET_applications_stats_job_site_사이트별_서류_통과율을_반환한다() throws Exception {
        given(applicationService.summarizeByJobSite(1L)).willReturn(java.util.List.of(
                new JobSiteStatisticsResponse(JobSite.SARAMIN, 10, 3, 0.3)));

        mockMvc.perform(get("/api/applications/stats/job-site").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobSite").value("SARAMIN"))
                .andExpect(jsonPath("$[0].applicationCount").value(10))
                .andExpect(jsonPath("$[0].documentPassedCount").value(3))
                .andExpect(jsonPath("$[0].documentPassRate").value(0.3));
    }

    @Test
    void 토큰이_없으면_사이트별_집계는_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/applications/stats/job-site"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 지원_건_응답에_서류_통과_시점이_담긴다() throws Exception {
        Application app = sampleApplication();
        app.startInterview(java.time.LocalDateTime.of(2026, 11, 5, 10, 0));
        given(applicationService.findById(1L, 1L)).willReturn(app);

        mockMvc.perform(get("/api/applications/1").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentPassed").value(true))
                .andExpect(jsonPath("$.documentPassedAt").value("2026-11-05T10:00:00"));
    }

    @Test
    void 존재하지_않거나_남의_지원_건은_404를_반환한다() throws Exception {
        given(applicationService.findById(1L, 1L)).willThrow(new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));

        mockMvc.perform(get("/api/applications/1").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void GET_applications_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void PATCH_applications_deadline_공고_마감일을_변경한다() throws Exception {
        Application app = sampleApplication();
        app.changeDeadline(LocalDate.of(2026, 11, 30));
        given(applicationService.changeDeadline(eq(1L), eq(1L), any())).willReturn(app);

        mockMvc.perform(patch("/api/applications/1/deadline")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deadline", "2026-11-30"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value("2026-11-30"));
    }

    @Test
    void 마감일이_없으면_마감일_변경은_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/applications/1/deadline")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 토큰이_없으면_마감일_변경은_401을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/applications/1/deadline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deadline", "2026-11-30"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void PATCH_applications_resume_view_열람_여부를_변경한다() throws Exception {
        Application app = sampleApplication();
        app.changeResumeView(ResumeView.VIEWED);
        given(applicationService.changeResumeView(eq(1L), eq(1L), any())).willReturn(app);

        mockMvc.perform(patch("/api/applications/1/resume-view")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resumeView", "VIEWED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeView").value("VIEWED"));
    }

    @Test
    void 열람_여부가_없으면_열람_여부_변경은_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/applications/1/resume-view")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 지원_건_응답에_마감일과_열람_여부와_자동_불합격_시각이_담긴다() throws Exception {
        Application app = Application.create(1L, Company.create("카카오", "IT", "https://kakao.com"), "백엔드 개발자",
                LocalDate.of(2026, 10, 1), JobSite.SARAMIN, LocalDate.of(2026, 10, 10), ResumeView.NOT_VIEWED);
        app.rejectIfDeadlineExpired(java.time.LocalDateTime.of(2026, 11, 11, 7, 0));
        given(applicationService.findById(1L, 1L)).willReturn(app);

        mockMvc.perform(get("/api/applications/1").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value("2026-10-10"))
                .andExpect(jsonPath("$.resumeView").value("NOT_VIEWED"))
                .andExpect(jsonPath("$.autoRejectedAt").value("2026-11-11T07:00:00"));
    }
}

package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.presentation.controller.ApplicationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class ApplicationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ApplicationService applicationService;

    private Application sampleApplication() {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        return Application.create(1L, company, "백엔드 개발자", LocalDate.now());
    }

    @Test
    void POST_applications_지원_건을_등록한다() throws Exception {
        given(applicationService.register(any())).willReturn(sampleApplication());

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("companyId", 1, "position", "백엔드 개발자", "appliedDate", LocalDate.now().toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESUME_SUBMITTED"));
    }

    @Test
    void PATCH_applications_status_상태를_전이한다() throws Exception {
        Application app = sampleApplication();
        app.advanceStatus(ApplicationStatus.INTERVIEW);
        given(applicationService.advanceStatus(eq(1L), any())).willReturn(app);

        mockMvc.perform(patch("/api/applications/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INTERVIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    @Test
    void GET_applications_status로_면접_단계_지원_건만_조회한다() throws Exception {
        Application app = sampleApplication();
        app.advanceStatus(ApplicationStatus.INTERVIEW);
        given(applicationService.findByStatus(ApplicationStatus.INTERVIEW)).willReturn(java.util.List.of(app));

        mockMvc.perform(get("/api/applications").param("status", "INTERVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INTERVIEW"));
    }
}

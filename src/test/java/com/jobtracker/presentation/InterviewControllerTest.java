package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.InterviewService;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.interview.Interview;
import com.jobtracker.presentation.controller.InterviewController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterviewController.class)
class InterviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean InterviewService interviewService;

    @Test
    void POST_interviews_면접을_등록한다() throws Exception {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        Interview interview = Interview.create(app, 1, LocalDateTime.now(), "판교", "1차 면접");
        given(interviewService.register(eq(1L), any())).willReturn(interview);

        mockMvc.perform(post("/api/applications/1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("round", 1, "interviewDate", LocalDateTime.now().toString(),
                                        "location", "판교", "note", "1차 면접"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.round").value(1));
    }
}

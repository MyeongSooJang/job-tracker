package com.ssangtteongnip.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.application.service.InterviewService;
import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.interview.Interview;
import com.ssangtteongnip.presentation.controller.InterviewController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterviewController.class)
@Import(SecurityConfig.class)
class InterviewControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @MockitoBean TokenProvider tokenProvider;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean InterviewService interviewService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    @Test
    void POST_interviews_면접을_등록한다() throws Exception {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        Interview interview = Interview.create(app, 1, LocalDateTime.now(), "판교", "1차 면접");
        given(interviewService.register(eq(1L), eq(1L), any())).willReturn(interview);

        mockMvc.perform(post("/api/applications/1/interviews")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("round", 1, "interviewDate", LocalDateTime.now().toString(),
                                        "location", "판교", "note", "1차 면접"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.round").value(1));
    }

    @Test
    void PATCH_score_면접_점수를_매긴다() throws Exception {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        Interview interview = Interview.create(app, 1, LocalDateTime.now(), "판교", "1차 면접");
        interview.evaluate(85);
        given(interviewService.evaluate(eq(1L), eq(1L), eq(1L), any())).willReturn(interview);

        mockMvc.perform(patch("/api/applications/1/interviews/1/score")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 85))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(85));
    }

    @Test
    void PATCH_score_100점을_초과하면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/applications/1/interviews/1/score")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", 101))))
                .andExpect(status().isBadRequest());
    }

    private Interview interview() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
        return Interview.create(app, 1, LocalDateTime.of(2026, 11, 10, 14, 0), "판교", "1차 면접");
    }

    @Test
    void 지원_건의_면접_목록을_조회한다() throws Exception {
        given(interviewService.findByApplicationId(1L, 1L)).willReturn(List.of(interview()));

        mockMvc.perform(get("/api/applications/1/interviews").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].round").value(1))
                .andExpect(jsonPath("$[0].location").value("판교"));
    }

    @Test
    void 면접_회차가_1_미만이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/applications/1/interviews")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("round", 0, "interviewDate", "2026-11-10T14:00:00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 면접에_질문을_추가하면_201과_질문을_반환한다() throws Exception {
        Interview interview = interview();
        interview.addQuestion("자기소개 해주세요", "저는...", "잘 대답했음");
        given(interviewService.addQuestion(eq(1L), eq(1L), eq(1L), any())).willReturn(interview);

        mockMvc.perform(post("/api/applications/1/interviews/1/questions")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("question", "자기소개 해주세요"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questions[0].question").value("자기소개 해주세요"));
    }

    @Test
    void 질문이_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/applications/1/interviews/1/questions")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("question", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}

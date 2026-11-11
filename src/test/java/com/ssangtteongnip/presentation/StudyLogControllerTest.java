package com.ssangtteongnip.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.application.service.StudyLogService;
import com.ssangtteongnip.application.service.dto.HeatmapDayResponse;
import com.ssangtteongnip.application.service.dto.StudyLogCreateRequest;
import com.ssangtteongnip.application.service.dto.StudyLogRequest;
import com.ssangtteongnip.application.service.dto.StudyLogResponse;
import com.ssangtteongnip.application.service.dto.StudyStatsResponse;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.presentation.controller.StudyLogController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyLogController.class)
@Import(SecurityConfig.class)
class StudyLogControllerTest {

    private static final String BEARER = "Bearer valid-token";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TokenProvider tokenProvider;
    @MockBean StudyLogService studyLogService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private StudyLogResponse response() {
        return new StudyLogResponse(10L, DATE, "JPA 정리", "JPA 공부", 90);
    }

    @Test
    void POST_글을_작성하고_201과_저장된_글을_반환한다() throws Exception {
        given(studyLogService.create(1L, new StudyLogCreateRequest(DATE, "JPA 정리", "JPA 공부", 90)))
                .willReturn(response());

        mockMvc.perform(post("/api/me/study-logs")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studyDate", "2026-11-03", "title", "JPA 정리", "content", "JPA 공부", "studyMinutes", 90))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.studyDate").value("2026-11-03"))
                .andExpect(jsonPath("$.title").value("JPA 정리"))
                .andExpect(jsonPath("$.content").value("JPA 공부"))
                .andExpect(jsonPath("$.studyMinutes").value(90));
    }

    @Test
    void POST_날짜가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/study-logs")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "제목", "content", "내용"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("날짜를 입력해주세요."));
    }

    @Test
    void POST_제목이_100자를_넘으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/study-logs")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studyDate", "2026-11-03", "title", "가".repeat(101), "content", "내용"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("제목은 100자 이하여야 합니다."));
    }

    @Test
    void POST_내용이_10000자를_넘으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/study-logs")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studyDate", "2026-11-03", "title", "제목", "content", "가".repeat(10001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("공부 내용은 10000자 이하여야 합니다."));
    }

    @Test
    void PUT_글을_수정하고_200과_수정된_글을_반환한다() throws Exception {
        given(studyLogService.rewrite(1L, 10L, new StudyLogRequest("JPA 정리", "JPA 공부", 90))).willReturn(response());

        mockMvc.perform(put("/api/me/study-logs/10")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "JPA 정리", "content", "JPA 공부", "studyMinutes", 90))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("JPA 정리"));
    }

    @Test
    void PUT_남의_글이거나_없는_글이면_404와_에러코드를_반환한다() throws Exception {
        given(studyLogService.rewrite(1L, 99L, new StudyLogRequest("제목", "내용", null)))
                .willThrow(new BusinessException(ErrorCode.STUDY_LOG_NOT_FOUND));

        mockMvc.perform(put("/api/me/study-logs/99")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "제목", "content", "내용"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_LOG_NOT_FOUND"));
    }

    @Test
    void GET_글_하나를_id로_조회한다() throws Exception {
        given(studyLogService.find(1L, 10L)).willReturn(response());

        mockMvc.perform(get("/api/me/study-logs/10").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void GET_없는_글은_404와_에러코드를_반환한다() throws Exception {
        given(studyLogService.find(1L, 99L)).willThrow(new BusinessException(ErrorCode.STUDY_LOG_NOT_FOUND));

        mockMvc.perform(get("/api/me/study-logs/99").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_LOG_NOT_FOUND"));
    }

    @Test
    void GET_기간을_지정하면_그_기간의_글_목록을_반환한다() throws Exception {
        given(studyLogService.findRange(1L, LocalDate.of(2026, 10, 21), DATE)).willReturn(List.of(response()));

        mockMvc.perform(get("/api/me/study-logs")
                        .param("from", "2026-10-21").param("to", "2026-11-03")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studyDate").value("2026-11-03"));
    }

    @Test
    void GET_기간이_상한을_넘으면_400과_에러코드를_반환한다() throws Exception {
        given(studyLogService.findRange(1L, LocalDate.of(2026, 1, 1), DATE))
                .willThrow(new BusinessException(ErrorCode.INVALID_STUDY_RANGE));

        mockMvc.perform(get("/api/me/study-logs")
                        .param("from", "2026-01-01").param("to", "2026-11-03")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STUDY_RANGE"));
    }

    @Test
    void GET_날짜_형식이_올바르지_않으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/study-logs")
                        .param("from", "2026-13-45").param("to", "2026-11-03")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_stats는_연속_작성일과_최장_연속과_오늘_공부_시간을_반환한다() throws Exception {
        given(studyLogService.stats(1L)).willReturn(new StudyStatsResponse(3, 21, 5, 90));

        mockMvc.perform(get("/api/me/study-logs/stats").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(3))
                .andExpect(jsonPath("$.longestStreak").value(21))
                .andExpect(jsonPath("$.monthWrittenDays").value(5))
                .andExpect(jsonPath("$.todayMinutes").value(90))
                .andExpect(jsonPath("$.last7Days").doesNotExist());
    }

    @Test
    void GET_heatmap은_글이_있는_날의_글_수와_공부_시간을_반환한다() throws Exception {
        given(studyLogService.heatmap(1L, LocalDate.of(2025, 11, 3), DATE))
                .willReturn(List.of(new HeatmapDayResponse(DATE, 2, 90)));

        mockMvc.perform(get("/api/me/study-logs/heatmap")
                        .param("from", "2025-11-03").param("to", "2026-11-03")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].date").value("2026-11-03"))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].minutes").value(90));
    }

    @Test
    void GET_heatmap은_기간이_상한을_넘으면_400을_반환한다() throws Exception {
        given(studyLogService.heatmap(1L, LocalDate.of(2020, 1, 1), DATE))
                .willThrow(new BusinessException(ErrorCode.INVALID_STUDY_RANGE));

        mockMvc.perform(get("/api/me/study-logs/heatmap")
                        .param("from", "2020-01-01").param("to", "2026-11-03")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STUDY_RANGE"));
    }

    @Test
    void DELETE_글을_삭제하고_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/me/study-logs/10").header("Authorization", BEARER))
                .andExpect(status().isNoContent());

        verify(studyLogService).delete(1L, 10L);
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/study-logs/stats"))
                .andExpect(status().isUnauthorized());
    }
}

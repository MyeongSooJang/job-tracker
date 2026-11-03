package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.StudyLogService;
import com.jobtracker.application.service.dto.StudyLogRequest;
import com.jobtracker.application.service.dto.StudyLogResponse;
import com.jobtracker.application.service.dto.StudyStatsResponse;
import com.jobtracker.domain.studylog.DailyStudyMinutes;
import com.jobtracker.domain.user.TokenProvider;
import com.jobtracker.global.config.SecurityConfig;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.presentation.controller.StudyLogController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        return new StudyLogResponse(10L, DATE, "JPA 공부", 90);
    }

    @Test
    void PUT_날짜별_기록을_저장하고_200과_저장된_기록을_반환한다() throws Exception {
        given(studyLogService.save(1L, DATE, new StudyLogRequest("JPA 공부", 90))).willReturn(response());

        mockMvc.perform(put("/api/me/study-logs/2026-11-03")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "JPA 공부", "studyMinutes", 90))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyDate").value("2026-11-03"))
                .andExpect(jsonPath("$.content").value("JPA 공부"))
                .andExpect(jsonPath("$.studyMinutes").value(90));
    }

    @Test
    void PUT_내용이_10000자를_넘으면_400을_반환한다() throws Exception {
        mockMvc.perform(put("/api/me/study-logs/2026-11-03")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "가".repeat(10001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("공부 내용은 10000자 이하여야 합니다."));
    }

    @Test
    void PUT_날짜_형식이_올바르지_않으면_400을_반환한다() throws Exception {
        mockMvc.perform(put("/api/me/study-logs/2026-13-45")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "JPA 공부"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_기록이_없는_날짜는_404와_에러코드를_반환한다() throws Exception {
        given(studyLogService.find(1L, DATE)).willThrow(new BusinessException(ErrorCode.STUDY_LOG_NOT_FOUND));

        mockMvc.perform(get("/api/me/study-logs/2026-11-03").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_LOG_NOT_FOUND"));
    }

    @Test
    void GET_월을_지정하면_그_달의_기록_목록을_반환한다() throws Exception {
        given(studyLogService.findByMonth(1L, YearMonth.of(2026, 11))).willReturn(List.of(response()));

        mockMvc.perform(get("/api/me/study-logs").param("month", "2026-11").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studyDate").value("2026-11-03"));
    }

    @Test
    void GET_월_형식이_올바르지_않으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/study-logs").param("month", "2026-13").header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_recent는_날짜가_아니라_최근_기록_목록으로_매핑된다() throws Exception {
        given(studyLogService.findRecent(1L)).willReturn(List.of(response()));

        mockMvc.perform(get("/api/me/study-logs/recent").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("JPA 공부"));
    }

    @Test
    void GET_stats는_연속_작성일과_최근_7일_공부_시간을_반환한다() throws Exception {
        given(studyLogService.stats(1L)).willReturn(
                new StudyStatsResponse(3, 5, List.of(new DailyStudyMinutes(DATE, 90))));

        mockMvc.perform(get("/api/me/study-logs/stats").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(3))
                .andExpect(jsonPath("$.monthWrittenDays").value(5))
                .andExpect(jsonPath("$.last7Days[0].date").value("2026-11-03"))
                .andExpect(jsonPath("$.last7Days[0].minutes").value(90));
    }

    @Test
    void DELETE_기록을_삭제하고_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/me/study-logs/2026-11-03").header("Authorization", BEARER))
                .andExpect(status().isNoContent());

        verify(studyLogService).delete(1L, DATE);
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/study-logs/stats"))
                .andExpect(status().isUnauthorized());
    }
}

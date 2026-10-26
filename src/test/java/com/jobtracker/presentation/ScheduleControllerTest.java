package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.ScheduleService;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleType;
import com.jobtracker.presentation.controller.ScheduleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ScheduleService scheduleService;

    @Test
    void POST_schedules_일정을_등록한다() throws Exception {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        Schedule schedule = Schedule.create(app, "1차 면접", LocalDateTime.now(), ScheduleType.INTERVIEW, "");
        given(scheduleService.register(any())).willReturn(schedule);

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "applicationId", 1,
                                "title", "1차 면접",
                                "scheduledAt", LocalDateTime.now().toString(),
                                "type", "INTERVIEW"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("1차 면접"));
    }

    @Test
    void GET_schedules_일정_목록을_조회한다() throws Exception {
        given(scheduleService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk());
    }
}

package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.CompanyService;
import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.presentation.controller.CompanyController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean CompanyService companyService;

    @Test
    void POST_companies_회사를_등록한다() throws Exception {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyService.register(any())).willReturn(company);

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "카카오", "industry", "IT", "website", "https://kakao.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("카카오"));
    }

    @Test
    void GET_companies_회사_목록을_조회한다() throws Exception {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyService.findAll()).willReturn(List.of(company));

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("카카오"));
    }

    @Test
    void GET_companies_id_회사를_단건_조회한다() throws Exception {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyService.findById(1L)).willReturn(company);

        mockMvc.perform(get("/api/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("카카오"));
    }

    @Test
    void GET_companies_id_존재하지_않으면_404를_반환한다() throws Exception {
        given(companyService.findById(1L))
                .willThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        mockMvc.perform(get("/api/companies/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }
}

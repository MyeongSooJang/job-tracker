package com.jobtracker.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.service.CompanyService;
import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.presentation.controller.CompanyController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.jobtracker.domain.user.TokenProvider;
import com.jobtracker.global.config.SecurityConfig;
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
@Import(SecurityConfig.class)
class CompanyControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @MockBean TokenProvider tokenProvider;
    @Autowired ObjectMapper objectMapper;
    @MockBean CompanyService companyService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    @Test
    void POST_companies_회사를_등록한다() throws Exception {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyService.register(any())).willReturn(company);

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", BEARER)
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

        mockMvc.perform(get("/api/companies")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("카카오"));
    }

    @Test
    void GET_companies_id_회사를_단건_조회한다() throws Exception {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyService.findById(1L)).willReturn(company);

        mockMvc.perform(get("/api/companies/1")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("카카오"));
    }

    @Test
    void GET_companies_id_존재하지_않으면_404를_반환한다() throws Exception {
        given(companyService.findById(1L))
                .willThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        mockMvc.perform(get("/api/companies/1")
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    void 회사_이름이_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", " ", "industry", "IT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 이미_등록된_회사는_409를_반환한다() throws Exception {
        given(companyService.register(any())).willThrow(new BusinessException(ErrorCode.COMPANY_ALREADY_EXISTS));

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "카카오"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANY_ALREADY_EXISTS"));
    }
}

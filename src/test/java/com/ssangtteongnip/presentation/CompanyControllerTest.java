package com.ssangtteongnip.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.application.service.CompanyService;
import com.ssangtteongnip.application.service.dto.CompanyResponse;
import com.ssangtteongnip.application.service.dto.CompanyReviewResponse;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.review.ApplyRoute;
import com.ssangtteongnip.domain.review.InterviewAtmosphere;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.presentation.controller.CompanyController;
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

import java.time.LocalDateTime;
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
    void GET_companies_회사_목록을_후기_수와_평균_평점과_함께_조회한다() throws Exception {
        given(companyService.findAll()).willReturn(List.of(
                companyResponse("카카오", 2, 4.5),
                companyResponse("네이버", 0, null)));

        mockMvc.perform(get("/api/companies")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("카카오"))
                .andExpect(jsonPath("$[0].reviewCount").value(2))
                .andExpect(jsonPath("$[0].averageRating").value(4.5))
                .andExpect(jsonPath("$[1].reviewCount").value(0))
                .andExpect(jsonPath("$[1].averageRating").doesNotExist());
    }

    @Test
    void GET_companies_id_회사를_단건_조회한다() throws Exception {
        given(companyService.findDetail(1L)).willReturn(companyResponse("카카오", 3, 4.0));

        mockMvc.perform(get("/api/companies/1")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("카카오"))
                .andExpect(jsonPath("$.reviewCount").value(3))
                .andExpect(jsonPath("$.averageRating").value(4.0));
    }

    @Test
    void GET_companies_id_존재하지_않으면_404를_반환한다() throws Exception {
        given(companyService.findDetail(1L))
                .willThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        mockMvc.perform(get("/api/companies/1")
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    void GET_companies_id_reviews_회사의_후기를_작성자_정보_없이_조회한다() throws Exception {
        given(companyService.findReviews(1L)).willReturn(List.of(
                new CompanyReviewResponse(10L, 5, "면접관이 친절했어요", "백엔드 개발자",
                        ApplicationStatus.ACCEPTED, 3, InterviewAtmosphere.COMFORTABLE, ApplyRoute.REFERRAL,
                        List.of("자기소개"), LocalDateTime.of(2026, 10, 20, 9, 0))));

        mockMvc.perform(get("/api/companies/1/reviews")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].content").value("면접관이 친절했어요"))
                .andExpect(jsonPath("$[0].position").value("백엔드 개발자"))
                .andExpect(jsonPath("$[0].stage").value("ACCEPTED"))
                .andExpect(jsonPath("$[0].difficulty").value(3))
                .andExpect(jsonPath("$[0].atmosphere").value("COMFORTABLE"))
                .andExpect(jsonPath("$[0].route").value("REFERRAL"))
                .andExpect(jsonPath("$[0].questions[0]").value("자기소개"))
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].application").doesNotExist());
    }

    @Test
    void GET_companies_id_reviews_존재하지_않는_회사면_404를_반환한다() throws Exception {
        given(companyService.findReviews(1L))
                .willThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        mockMvc.perform(get("/api/companies/1/reviews")
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    void GET_companies_id_reviews_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/companies/1/reviews"))
                .andExpect(status().isUnauthorized());
    }

    private CompanyResponse companyResponse(String name, long reviewCount, Double averageRating) {
        return new CompanyResponse(1L, name, "IT", "https://example.com",
                LocalDateTime.of(2026, 10, 1, 9, 0), reviewCount, averageRating);
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

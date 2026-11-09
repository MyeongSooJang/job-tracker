package com.ssangtteongnip.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.application.service.ReviewService;
import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.review.ApplyRoute;
import com.ssangtteongnip.domain.review.InterviewAtmosphere;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.presentation.controller.ReviewController;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @MockBean TokenProvider tokenProvider;
    @Autowired ObjectMapper objectMapper;
    @MockBean ReviewService reviewService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private Review review() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application app = Application.create(1L, company, "백엔드 개발자", LocalDate.now());
        return Review.create(app);
    }

    @Test
    void GET_reviews_후기_목록을_조회한다() throws Exception {
        given(reviewService.findAll(1L)).willReturn(List.of(review()));

        mockMvc.perform(get("/api/reviews")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].application.position").value("백엔드 개발자"));
    }

    @Test
    void PATCH_reviews_후기를_작성한다() throws Exception {
        Review review = review();
        review.write("분위기가 좋았다", 4);
        given(reviewService.write(eq(1L), eq(1L), any())).willReturn(review);

        mockMvc.perform(patch("/api/reviews/1")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "분위기가 좋았다", "rating", 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("분위기가 좋았다"))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void PATCH_reviews_면접_상세를_함께_받아_돌려준다() throws Exception {
        Review review = review();
        review.write("분위기가 좋았다", 4);
        review.describeInterview(3, InterviewAtmosphere.COMFORTABLE, ApplyRoute.REFERRAL, List.of("자기소개"));
        given(reviewService.write(eq(1L), eq(1L), any())).willReturn(review);

        mockMvc.perform(patch("/api/reviews/1")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "분위기가 좋았다", "rating", 4,
                                "difficulty", 3, "atmosphere", "COMFORTABLE",
                                "route", "REFERRAL", "questions", List.of("자기소개")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value(3))
                .andExpect(jsonPath("$.atmosphere").value("COMFORTABLE"))
                .andExpect(jsonPath("$.route").value("REFERRAL"))
                .andExpect(jsonPath("$.questions[0]").value("자기소개"));
    }

    @Test
    void PATCH_reviews_난이도가_범위를_벗어나면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/reviews/1")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "좋았다", "rating", 4, "difficulty", 6))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void PATCH_reviews_내용이_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/reviews/1")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "", "rating", 4))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void PATCH_reviews_평점이_범위를_벗어나면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/reviews/1")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "좋았다", "rating", 6))))
                .andExpect(status().isBadRequest());
    }
}

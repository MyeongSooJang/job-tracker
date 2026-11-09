package com.ssangtteongnip.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.application.service.ProfileService;
import com.ssangtteongnip.application.service.dto.ProfileResponse;
import com.ssangtteongnip.application.service.dto.ResumeResponse;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import com.ssangtteongnip.presentation.controller.ProfileController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TokenProvider tokenProvider;
    @MockBean ProfileService profileService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private static ProfileResponse profileResponse(ResumeResponse resume) {
        return new ProfileResponse("hong@example.com", "홍길동", "010-1234-5678", null, null,
                null, null, null, null, null, "백엔드 개발자", null, LocalDate.of(2025, 3, 1), resume);
    }

    @Test
    void GET_me_profile_내_프로필과_이력서_정보를_조회한다() throws Exception {
        ResumeResponse resume = new ResumeResponse("이력서.pdf", 1024, LocalDateTime.of(2026, 11, 1, 10, 0));
        given(profileService.getMyProfile(1L)).willReturn(profileResponse(resume));

        mockMvc.perform(get("/api/me/profile").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("hong@example.com"))
                .andExpect(jsonPath("$.desiredJob").value("백엔드 개발자"))
                .andExpect(jsonPath("$.jobSearchStartDate").value("2025-03-01"))
                .andExpect(jsonPath("$.resume.fileName").value("이력서.pdf"))
                .andExpect(jsonPath("$.resume.sizeBytes").value(1024));
    }

    @Test
    void GET_me_profile_이력서가_없으면_resume이_null이다() throws Exception {
        given(profileService.getMyProfile(1L)).willReturn(profileResponse(null));

        mockMvc.perform(get("/api/me/profile").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume").doesNotExist());
    }

    @Test
    void GET_me_profile_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void GET_me_profile_유효하지_않은_토큰이면_401을_반환한다() throws Exception {
        given(tokenProvider.parseUserId("bad-token"))
                .willThrow(new com.ssangtteongnip.global.exception.BusinessException(
                        com.ssangtteongnip.global.exception.ErrorCode.UNAUTHORIZED));

        mockMvc.perform(get("/api/me/profile").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void PUT_me_profile_인적_사항을_저장한다() throws Exception {
        given(profileService.updateMyProfile(eq(1L), any())).willReturn(profileResponse(null));

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "홍길동", "phone", "010-1234-5678", "desiredJob", "백엔드 개발자"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"));
    }

    @Test
    void PUT_me_profile_이름이_비어_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void PUT_me_profile_취업_준비_시작일을_저장한다() throws Exception {
        given(profileService.updateMyProfile(eq(1L), any())).willReturn(profileResponse(null));

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "홍길동", "jobSearchStartDate", "2025-03-01"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobSearchStartDate").value("2025-03-01"));
    }

    @Test
    void PUT_me_profile_취업_준비_시작일이_미래이면_400을_반환한다() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "홍길동", "jobSearchStartDate", tomorrow))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void PUT_me_profile_취업_준비_시작일이_오늘이면_저장한다() throws Exception {
        given(profileService.updateMyProfile(eq(1L), any())).willReturn(profileResponse(null));

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "홍길동", "jobSearchStartDate", LocalDate.now().toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void PUT_me_profile_취업_준비_시작일을_생략해도_저장한다() throws Exception {
        given(profileService.updateMyProfile(eq(1L), any())).willReturn(profileResponse(null));

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "홍길동"))))
                .andExpect(status().isOk());
    }
}

package com.ssangtteongnip.presentation;

import com.ssangtteongnip.application.service.ResumeService;
import com.ssangtteongnip.application.service.dto.ResumeDownload;
import com.ssangtteongnip.application.service.dto.ResumeResponse;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.presentation.controller.ResumeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
@Import(SecurityConfig.class)
class ResumeControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @MockitoBean TokenProvider tokenProvider;
    @MockitoBean ResumeService resumeService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private static MockMultipartHttpServletRequestBuilder putMultipart() {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/me/resume")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
    }

    private static MockMultipartFile pdfFile() {
        return new MockMultipartFile("file", "이력서.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 body".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void PUT_me_resume_PDF를_업로드한다() throws Exception {
        given(resumeService.upload(eq(1L), eq("이력서.pdf"), any()))
                .willReturn(new ResumeResponse("이력서.pdf", 13, LocalDateTime.of(2026, 11, 1, 10, 0)));

        mockMvc.perform(putMultipart().file(pdfFile()).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("이력서.pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(13));
    }

    @Test
    void PUT_me_resume_PDF가_아니면_400을_반환한다() throws Exception {
        given(resumeService.upload(eq(1L), any(), any()))
                .willThrow(new BusinessException(ErrorCode.INVALID_FILE_TYPE));

        mockMvc.perform(putMultipart()
                        .file(new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes()))
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void PUT_me_resume_file_파트가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(putMultipart().header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_FILE"));
    }

    @Test
    void PUT_me_resume_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(putMultipart().file(pdfFile()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_me_resume_내려받기_주소를_JSON으로_반환한다() throws Exception {
        given(resumeService.download(1L)).willReturn(new ResumeDownload(
                "https://test-bucket.s3.amazonaws.com/resumes/1/a.pdf?X-Amz-Signature=x",
                "이력서.pdf",
                Instant.parse("2026-11-05T01:05:00Z")));

        mockMvc.perform(get("/api/me/resume").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://test-bucket.s3.amazonaws.com/resumes/1/a.pdf?X-Amz-Signature=x"))
                .andExpect(jsonPath("$.fileName").value("이력서.pdf"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-05T01:05:00Z"));
    }

    @Test
    void GET_me_resume_이력서가_없으면_404를_반환한다() throws Exception {
        given(resumeService.download(1L)).willThrow(new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        mockMvc.perform(get("/api/me/resume").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }

    @Test
    void DELETE_me_resume_이력서를_삭제한다() throws Exception {
        mockMvc.perform(delete("/api/me/resume").header("Authorization", BEARER))
                .andExpect(status().isNoContent());
    }

    @Test
    void DELETE_me_resume_이력서가_없으면_404를_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.RESUME_NOT_FOUND)).given(resumeService).delete(1L);

        mockMvc.perform(delete("/api/me/resume").header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }
}

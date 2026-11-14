package com.ssangtteongnip.presentation;

import com.ssangtteongnip.application.service.UserFileService;
import com.ssangtteongnip.application.service.dto.FileDownload;
import com.ssangtteongnip.application.service.dto.UserFileResponse;
import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.presentation.controller.UserFileController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserFileController.class)
@Import(SecurityConfig.class)
class UserFileControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @MockitoBean TokenProvider tokenProvider;
    @MockitoBean UserFileService userFileService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private static MockMultipartFile pdfFile() {
        return new MockMultipartFile("file", "포트폴리오.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 body".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void POST_me_files_종류와_함께_PDF를_업로드한다() throws Exception {
        given(userFileService.upload(eq(1L), eq(FileType.PORTFOLIO), eq("포트폴리오.pdf"), any()))
                .willReturn(new UserFileResponse(5L, FileType.PORTFOLIO, "포트폴리오.pdf", 13,
                        LocalDateTime.of(2026, 11, 13, 10, 0)));

        mockMvc.perform(multipart("/api/me/files").file(pdfFile()).param("type", "PORTFOLIO")
                        .header("Authorization", BEARER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.type").value("PORTFOLIO"))
                .andExpect(jsonPath("$.fileName").value("포트폴리오.pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(13));
    }

    @Test
    void POST_me_files_종류가_없으면_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(multipart("/api/me/files").file(pdfFile()).header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_CATEGORY"));

        verifyNoInteractions(userFileService);
    }

    @Test
    void POST_me_files_알_수_없는_종류면_400을_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/me/files").file(pdfFile()).param("type", "DIPLOMA")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_CATEGORY"));
    }

    @Test
    void POST_me_files_한도를_넘으면_400을_반환한다() throws Exception {
        given(userFileService.upload(eq(1L), any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.FILE_LIMIT_EXCEEDED));

        mockMvc.perform(multipart("/api/me/files").file(pdfFile()).param("type", "RESUME")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_LIMIT_EXCEEDED"));
    }

    @Test
    void POST_me_files_file_파트가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/me/files").param("type", "RESUME").header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_FILE"));
    }

    @Test
    void POST_me_files_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/me/files").file(pdfFile()).param("type", "RESUME"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_me_files_목록을_종류와_함께_반환한다() throws Exception {
        given(userFileService.list(1L)).willReturn(List.of(
                new UserFileResponse(6L, FileType.CERTIFICATE, "자격증.pdf", 20, LocalDateTime.of(2026, 11, 13, 11, 0)),
                new UserFileResponse(5L, FileType.RESUME, "이력서.pdf", 10, LocalDateTime.of(2026, 11, 13, 10, 0))));

        mockMvc.perform(get("/api/me/files").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(6))
                .andExpect(jsonPath("$[0].type").value("CERTIFICATE"))
                .andExpect(jsonPath("$[1].type").value("RESUME"));
    }

    @Test
    void GET_me_files_id_내려받기_주소를_JSON으로_반환한다() throws Exception {
        given(userFileService.download(1L, 5L)).willReturn(new FileDownload(
                "https://test-bucket.s3.amazonaws.com/files/1/a.pdf?X-Amz-Signature=x",
                "이력서.pdf",
                Instant.parse("2026-11-05T01:05:00Z")));

        mockMvc.perform(get("/api/me/files/5").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://test-bucket.s3.amazonaws.com/files/1/a.pdf?X-Amz-Signature=x"))
                .andExpect(jsonPath("$.fileName").value("이력서.pdf"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-05T01:05:00Z"));
    }

    @Test
    void GET_me_files_id_파일이_없으면_404를_반환한다() throws Exception {
        given(userFileService.download(1L, 5L)).willThrow(new BusinessException(ErrorCode.FILE_NOT_FOUND));

        mockMvc.perform(get("/api/me/files/5").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }

    @Test
    void DELETE_me_files_id_파일을_삭제한다() throws Exception {
        mockMvc.perform(delete("/api/me/files/5").header("Authorization", BEARER))
                .andExpect(status().isNoContent());
    }

    @Test
    void DELETE_me_files_id_파일이_없으면_404를_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.FILE_NOT_FOUND)).given(userFileService).delete(1L, 5L);

        mockMvc.perform(delete("/api/me/files/5").header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }
}

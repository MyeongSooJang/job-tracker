package com.ssangtteongnip.presentation;

import com.ssangtteongnip.application.service.StudyImageService;
import com.ssangtteongnip.application.service.dto.StudyImageResponse;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.presentation.controller.StudyImageController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyImageController.class)
@Import(SecurityConfig.class)
class StudyImageControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @MockBean TokenProvider tokenProvider;
    @MockBean StudyImageService studyImageService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void POST_me_images_이미지를_올리면_201과_본문에_넣을_경로를_반환한다() throws Exception {
        given(studyImageService.upload(eq(1L), any())).willReturn(new StudyImageResponse("/api/images/abc"));

        mockMvc.perform(multipart("/api/me/images").file(image()).header("Authorization", BEARER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/api/images/abc"));
    }

    @Test
    void POST_me_images_이미지가_아니면_400을_반환한다() throws Exception {
        given(studyImageService.upload(eq(1L), any())).willThrow(new BusinessException(ErrorCode.INVALID_IMAGE_TYPE));

        mockMvc.perform(multipart("/api/me/images").file(image()).header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE_TYPE"));
    }

    @Test
    void POST_me_images_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/me/images").file(image()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void POST_me_images_file_파트가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/me/images").header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_images_토큰이_없어도_저장소_주소로_302_리다이렉트한다() throws Exception {
        UUID id = UUID.randomUUID();
        given(studyImageService.view(id))
                .willReturn(new PresignedUrl("https://bucket.example/study-images/1/a.png?sig=1", Instant.now()));

        mockMvc.perform(get("/api/images/" + id))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://bucket.example/study-images/1/a.png?sig=1"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, private"));
    }

    @Test
    void GET_images_없는_이미지는_404를_반환한다() throws Exception {
        UUID id = UUID.randomUUID();
        given(studyImageService.view(id)).willThrow(new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        mockMvc.perform(get("/api/images/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));
    }

    @Test
    void GET_images_형식이_다른_아이디는_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/images/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}

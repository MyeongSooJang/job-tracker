package com.ssangtteongnip.global.config;

import com.ssangtteongnip.application.service.CompanyService;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.presentation.controller.CompanyController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class CorsConfigTest {

    private static final String API_PATH = "/api/companies";

    @Autowired MockMvc mockMvc;
    @MockBean TokenProvider tokenProvider;
    @MockBean CompanyService companyService;

    @ParameterizedTest
    @ValueSource(strings = {
            "https://ssangtteongnip.com",
            "https://www.ssangtteongnip.com",
            "http://localhost:5173",
            "https://job-tracker-frontend-beryl-two.vercel.app",
            "https://preview-branch.vercel.app"
    })
    void 허용된_출처의_사전_요청은_해당_출처를_허용한다(String origin) throws Exception {
        mockMvc.perform(options(API_PATH)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.example",
            "https://ssangtteongnip.com.evil.example",
            "http://ssangtteongnip.com"
    })
    void 허용되지_않은_출처의_사전_요청은_거절한다(String origin) throws Exception {
        mockMvc.perform(options(API_PATH)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}

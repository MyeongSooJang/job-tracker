package com.jobtracker.domain;

import com.jobtracker.domain.company.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyTest {

    @Test
    void create_회사를_생성한다() {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");

        assertThat(company.getName()).isEqualTo("카카오");
        assertThat(company.getIndustry()).isEqualTo("IT");
        assertThat(company.getWebsite()).isEqualTo("https://kakao.com");
        assertThat(company.getUserId()).isEqualTo(1L);
    }

    @Test
    void update_회사_정보를_수정한다() {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        company.update("카카오뱅크", "핀테크", "https://kakaobank.com");

        assertThat(company.getName()).isEqualTo("카카오뱅크");
        assertThat(company.getIndustry()).isEqualTo("핀테크");
        assertThat(company.getWebsite()).isEqualTo("https://kakaobank.com");
    }
}

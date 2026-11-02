package com.jobtracker.application;

import com.jobtracker.application.service.CompanyService;
import com.jobtracker.application.service.dto.CompanyRequest;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.company.CompanyRepository;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @InjectMocks CompanyService companyService;

    @Test
    void 새로운_회사는_입력한_정보로_등록된다() {
        given(companyRepository.existsByName("카카오")).willReturn(false);
        given(companyRepository.save(any(Company.class))).willAnswer(i -> i.getArgument(0));

        Company registered = companyService.register(new CompanyRequest("카카오", "IT", "https://kakao.com"));

        assertThat(registered.getName()).isEqualTo("카카오");
        assertThat(registered.getIndustry()).isEqualTo("IT");
        assertThat(registered.getWebsite()).isEqualTo("https://kakao.com");
    }

    @Test
    void 이미_등록된_회사는_다시_등록할_수_없다() {
        given(companyRepository.existsByName("카카오")).willReturn(true);

        assertErrorCode(
                () -> companyService.register(new CompanyRequest("카카오", "IT", "https://kakao.com")),
                ErrorCode.COMPANY_ALREADY_EXISTS);
        verify(companyRepository, never()).save(any());
    }

    @Test
    void 존재하는_회사는_id로_조회할_수_있다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyRepository.findById(1L)).willReturn(Optional.of(company));

        assertThat(companyService.findById(1L)).isSameAs(company);
    }

    @Test
    void 존재하지_않는_회사는_조회할_수_없다() {
        given(companyRepository.findById(1L)).willReturn(Optional.empty());

        assertErrorCode(() -> companyService.findById(1L), ErrorCode.COMPANY_NOT_FOUND);
    }
}

package com.jobtracker.application;

import com.jobtracker.application.service.CompanyService;
import com.jobtracker.application.service.dto.CompanyRequest;
import com.jobtracker.domain.company.CompanyRepository;
import com.jobtracker.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @InjectMocks CompanyService companyService;

    @Test
    void register_이미_등록된_회사면_예외가_발생한다() {
        given(companyRepository.existsByName("카카오")).willReturn(true);

        assertThatThrownBy(() -> companyService.register(new CompanyRequest("카카오", "IT", "https://kakao.com")))
                .isInstanceOf(BusinessException.class);
        verify(companyRepository, never()).save(any());
    }
}

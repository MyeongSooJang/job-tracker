package com.jobtracker.application;

import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.application.service.CompanyService;
import com.jobtracker.application.service.dto.ApplicationStatusRequest;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationRepository;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock CompanyService companyService;
    @InjectMocks ApplicationService applicationService;

    private Application application() {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        return Application.create(1L, company, "백엔드 개발자", LocalDate.now());
    }

    @Test
    void advanceStatus_면접으로는_직접_전환할_수_없다() {
        assertThatThrownBy(() -> applicationService.advanceStatus(1L, new ApplicationStatusRequest(ApplicationStatus.INTERVIEW)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void advanceStatus_불합격은_직접_전환할_수_있다() {
        Application app = application();
        given(applicationRepository.findById(1L)).willReturn(Optional.of(app));

        applicationService.advanceStatus(1L, new ApplicationStatusRequest(ApplicationStatus.REJECTED));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }
}

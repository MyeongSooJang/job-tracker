package com.jobtracker.application;

import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.application.service.CompanyService;
import com.jobtracker.application.service.dto.ApplicationRequest;
import com.jobtracker.application.service.dto.ApplicationStatusRequest;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationRepository;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock CompanyService companyService;
    @InjectMocks ApplicationService applicationService;

    private Application application() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return Application.create(1L, company, "백엔드 개발자", LocalDate.now());
    }

    @Test
    void advanceStatus_면접으로는_직접_전환할_수_없다() {
        assertThatThrownBy(() -> applicationService.advanceStatus(1L, 1L, new ApplicationStatusRequest(ApplicationStatus.INTERVIEW)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void advanceStatus_불합격은_직접_전환할_수_있다() {
        Application app = application();
        given(applicationRepository.findById(1L)).willReturn(Optional.of(app));

        applicationService.advanceStatus(1L, 1L, new ApplicationStatusRequest(ApplicationStatus.REJECTED));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void findById_다른_사용자의_지원_건은_조회할_수_없다() {
        given(applicationRepository.findById(1L)).willReturn(Optional.of(application()));

        assertThatThrownBy(() -> applicationService.findById(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void advanceStatus_다른_사용자의_지원_건은_상태를_바꿀_수_없다() {
        Application app = application();
        given(applicationRepository.findById(1L)).willReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.advanceStatus(2L, 1L, new ApplicationStatusRequest(ApplicationStatus.REJECTED)))
                .isInstanceOf(BusinessException.class);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void register_인증된_사용자의_지원_건으로_저장한다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyService.findById(1L)).willReturn(company);
        given(applicationRepository.save(any(Application.class))).willAnswer(i -> i.getArgument(0));

        Application saved = applicationService.register(7L,
                new ApplicationRequest(1L, "백엔드 개발자", LocalDate.now()));

        assertThat(saved.isOwnedBy(7L)).isTrue();
    }
}

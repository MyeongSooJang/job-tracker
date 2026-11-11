package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.ApplicationService;
import com.ssangtteongnip.application.service.CompanyService;
import com.ssangtteongnip.application.service.dto.ApplicationJobSiteRequest;
import com.ssangtteongnip.application.service.dto.ApplicationRequest;
import com.ssangtteongnip.application.service.dto.ApplicationStatusRequest;
import com.ssangtteongnip.application.service.dto.JobSiteStatisticsResponse;
import com.ssangtteongnip.domain.application.JobSiteStatistics;
import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.application.ApplicationRepository;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.application.JobSite;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long APPLICATION_ID = 10L;

    @Mock ApplicationRepository applicationRepository;
    @Mock CompanyService companyService;
    @InjectMocks ApplicationService applicationService;

    private Application application() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return Application.create(OWNER_ID, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
    }

    @Test
    void 인증된_사용자의_지원_건으로_등록된다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyService.findById(1L)).willReturn(company);
        given(applicationRepository.save(any(Application.class))).willAnswer(i -> i.getArgument(0));

        Application saved = applicationService.register(7L,
                new ApplicationRequest(1L, "백엔드 개발자", LocalDate.of(2026, 11, 1), JobSite.SARAMIN));

        assertThat(saved.isOwnedBy(7L)).isTrue();
        assertThat(saved.getCompany()).isSameAs(company);
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
        assertThat(saved.getJobSite()).isEqualTo(JobSite.SARAMIN);
    }

    @Test
    void 소유자는_자신의_지원_건을_조회할_수_있다() {
        Application app = application();
        given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        Application found = applicationService.findById(OWNER_ID, APPLICATION_ID);

        assertThat(found).isSameAs(app);
    }

    @Test
    void 소유자는_자신의_지원_건의_구직_사이트를_바꿀_수_있다() {
        Application app = application();
        given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        applicationService.changeJobSite(OWNER_ID, APPLICATION_ID, new ApplicationJobSiteRequest(JobSite.GROUPBY));

        assertThat(app.getJobSite()).isEqualTo(JobSite.GROUPBY);
    }

    @Test
    void 다른_사용자의_지원_건은_구직_사이트를_바꿀_수_없다() {
        Application app = application();
        app.changeJobSite(JobSite.WANTED);
        given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        assertErrorCode(
                () -> applicationService.changeJobSite(OTHER_USER_ID, APPLICATION_ID, new ApplicationJobSiteRequest(JobSite.OTHER)),
                ErrorCode.APPLICATION_NOT_FOUND);
        assertThat(app.getJobSite()).isEqualTo(JobSite.WANTED);
    }

    @Test
    void 존재하지_않는_지원_건은_조회할_수_없다() {
        given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> applicationService.findById(OWNER_ID, APPLICATION_ID), ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void 다른_사용자의_지원_건은_조회할_수_없다() {
        given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application()));

        assertErrorCode(() -> applicationService.findById(OTHER_USER_ID, APPLICATION_ID), ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void 사이트별_집계는_서류_통과율을_계산해_응답한다() {
        given(applicationRepository.summarizeByJobSite(OWNER_ID)).willReturn(List.of(
                new JobSiteStatistics(JobSite.SARAMIN, 10, 3),
                new JobSiteStatistics(JobSite.WANTED, 2, 0)));

        List<JobSiteStatisticsResponse> responses = applicationService.summarizeByJobSite(OWNER_ID);

        assertThat(responses).containsExactly(
                new JobSiteStatisticsResponse(JobSite.SARAMIN, 10, 3, 0.3),
                new JobSiteStatisticsResponse(JobSite.WANTED, 2, 0, 0.0));
    }

    @Test
    void 불합격은_직접_전환할_수_있다() {
        Application app = application();
        given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        applicationService.advanceStatus(OWNER_ID, APPLICATION_ID, new ApplicationStatusRequest(ApplicationStatus.REJECTED));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void 면접으로는_직접_전환할_수_없고_면접_일정을_등록해야_한다() {
        Application app = application();
        given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        assertErrorCode(
                () -> applicationService.advanceStatus(OWNER_ID, APPLICATION_ID, new ApplicationStatusRequest(ApplicationStatus.INTERVIEW)),
                ErrorCode.INTERVIEW_SCHEDULE_REQUIRED);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void 다른_사용자의_지원_건은_상태를_바꿀_수_없다() {
        Application app = application();
        given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        assertErrorCode(
                () -> applicationService.advanceStatus(OTHER_USER_ID, APPLICATION_ID, new ApplicationStatusRequest(ApplicationStatus.REJECTED)),
                ErrorCode.APPLICATION_NOT_FOUND);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
    }
}

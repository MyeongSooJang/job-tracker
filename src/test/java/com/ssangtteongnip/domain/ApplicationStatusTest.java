package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @ParameterizedTest(name = "{0} → {1} 전이 허용 여부: {2}")
    @CsvSource({
            "RESUME_SUBMITTED, RESUME_SUBMITTED, false",
            "RESUME_SUBMITTED, INTERVIEW,        true",
            "RESUME_SUBMITTED, ACCEPTED,         false",
            "RESUME_SUBMITTED, REJECTED,         true",
            "INTERVIEW,        RESUME_SUBMITTED, false",
            "INTERVIEW,        INTERVIEW,        false",
            "INTERVIEW,        ACCEPTED,         true",
            "INTERVIEW,        REJECTED,         true",
            "ACCEPTED,         RESUME_SUBMITTED, false",
            "ACCEPTED,         INTERVIEW,        false",
            "ACCEPTED,         ACCEPTED,         false",
            "ACCEPTED,         REJECTED,         false",
            "REJECTED,         RESUME_SUBMITTED, false",
            "REJECTED,         INTERVIEW,        false",
            "REJECTED,         ACCEPTED,         false",
            "REJECTED,         REJECTED,         false"
    })
    void 상태_전이_규칙을_따른다(ApplicationStatus from, ApplicationStatus to, boolean allowed) {
        assertThat(from.canAdvanceTo(to)).isEqualTo(allowed);
    }

    private Application applicationInResumeSubmitted() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return Application.create(1L, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
    }

    private Application applicationInInterview() {
        Application app = applicationInResumeSubmitted();
        app.startInterview(LocalDateTime.of(2026, 11, 5, 10, 0));
        return app;
    }

    @Test
    void 합격을_요청하면_면접_중인_지원이_합격이_된다() {
        Application app = applicationInInterview();

        ApplicationStatus.ACCEPTED.applyTo(app);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void 불합격을_요청하면_지원이_불합격이_된다() {
        Application app = applicationInResumeSubmitted();

        ApplicationStatus.REJECTED.applyTo(app);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void 면접으로는_직접_요청할_수_없고_면접_일정을_등록해야_한다() {
        Application app = applicationInResumeSubmitted();

        assertErrorCode(() -> ApplicationStatus.INTERVIEW.applyTo(app), ErrorCode.INTERVIEW_SCHEDULE_REQUIRED);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
        assertThat(app.isDocumentPassed()).isFalse();
    }

    @Test
    void 서류_제출_상태로는_되돌릴_수_없다() {
        Application app = applicationInInterview();

        assertErrorCode(() -> ApplicationStatus.RESUME_SUBMITTED.applyTo(app), ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }
}

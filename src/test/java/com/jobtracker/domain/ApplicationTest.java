package com.jobtracker.domain;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ApplicationTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private Application applicationInResumeSubmitted() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return Application.create(OWNER_ID, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
    }

    private Application applicationInInterview() {
        Application app = applicationInResumeSubmitted();
        app.advanceStatus(ApplicationStatus.INTERVIEW);
        return app;
    }

    @Test
    void 지원을_새로_만들면_서류_제출_상태이다() {
        Application app = applicationInResumeSubmitted();

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void 서류_제출_상태의_지원은_면접으로_전이할_수_있다() {
        Application app = applicationInResumeSubmitted();

        app.advanceStatus(ApplicationStatus.INTERVIEW);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void 면접_중인_지원은_합격으로_전이할_수_있다() {
        Application app = applicationInInterview();

        app.advanceStatus(ApplicationStatus.ACCEPTED);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void 서류_제출_상태의_지원은_불합격으로_전이할_수_있다() {
        Application app = applicationInResumeSubmitted();

        app.advanceStatus(ApplicationStatus.REJECTED);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void 면접_중인_지원은_불합격으로_전이할_수_있다() {
        Application app = applicationInInterview();

        app.advanceStatus(ApplicationStatus.REJECTED);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void 서류_제출_상태에서_바로_합격으로_전이할_수_없다() {
        Application app = applicationInResumeSubmitted();

        assertErrorCode(() -> app.advanceStatus(ApplicationStatus.ACCEPTED), ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void 이미_합격한_지원은_다시_전이할_수_없다() {
        Application app = applicationInInterview();
        app.advanceStatus(ApplicationStatus.ACCEPTED);

        assertErrorCode(() -> app.advanceStatus(ApplicationStatus.REJECTED), ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void 지원한_본인은_소유자로_확인된다() {
        assertThat(applicationInResumeSubmitted().isOwnedBy(OWNER_ID)).isTrue();
    }

    @Test
    void 다른_사용자는_소유자로_확인되지_않는다() {
        assertThat(applicationInResumeSubmitted().isOwnedBy(OTHER_USER_ID)).isFalse();
    }

    @Test
    void 소유자는_소유권_검증을_통과한다() {
        Application app = applicationInResumeSubmitted();

        assertThatCode(() -> app.verifyOwner(OWNER_ID)).doesNotThrowAnyException();
    }

    @Test
    void 다른_사용자의_지원은_존재하지_않는_것으로_취급한다() {
        Application app = applicationInResumeSubmitted();

        assertErrorCode(() -> app.verifyOwner(OTHER_USER_ID), ErrorCode.APPLICATION_NOT_FOUND);
    }
}

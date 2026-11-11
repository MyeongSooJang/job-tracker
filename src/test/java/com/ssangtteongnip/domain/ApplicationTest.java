package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.application.JobSite;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final LocalDateTime PASSED_AT = LocalDateTime.of(2026, 11, 5, 10, 0);

    private Application applicationInResumeSubmitted() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return Application.create(OWNER_ID, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
    }

    private Application applicationInInterview() {
        Application app = applicationInResumeSubmitted();
        app.startInterview(PASSED_AT);
        return app;
    }

    @Test
    void 지원을_새로_만들면_서류_제출_상태이다() {
        Application app = applicationInResumeSubmitted();

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void 사이트를_모르던_지원도_구직_사이트를_지정할_수_있다() {
        Application app = applicationInResumeSubmitted();

        app.changeJobSite(JobSite.WANTED);

        assertThat(app.getJobSite()).isEqualTo(JobSite.WANTED);
    }

    @Test
    void 지정한_구직_사이트는_다른_사이트로_바꿀_수_있다() {
        Application app = applicationInResumeSubmitted();
        app.changeJobSite(JobSite.WANTED);

        app.changeJobSite(JobSite.OTHER);

        assertThat(app.getJobSite()).isEqualTo(JobSite.OTHER);
    }

    @Test
    void 구직_사이트를_비워서_바꿀_수는_없다() {
        Application app = applicationInResumeSubmitted();
        app.changeJobSite(JobSite.WANTED);

        assertThatThrownBy(() -> app.changeJobSite(null)).isInstanceOf(NullPointerException.class);
        assertThat(app.getJobSite()).isEqualTo(JobSite.WANTED);
    }

    @Test
    void 서류_제출_상태의_지원은_면접으로_전이할_수_있다() {
        Application app = applicationInResumeSubmitted();

        app.startInterview(PASSED_AT);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void 면접으로_전이하면_서류_통과_시점이_기록된다() {
        Application app = applicationInResumeSubmitted();

        app.startInterview(PASSED_AT);

        assertThat(app.getDocumentPassedAt()).isEqualTo(PASSED_AT);
        assertThat(app.isDocumentPassed()).isTrue();
    }

    @Test
    void 새로_만든_지원은_서류_통과_전이다() {
        assertThat(applicationInResumeSubmitted().isDocumentPassed()).isFalse();
    }

    @Test
    void 면접_후_불합격이_되어도_서류_통과_기록은_유지된다() {
        Application app = applicationInInterview();

        app.advanceStatus(ApplicationStatus.REJECTED);

        assertThat(app.isDocumentPassed()).isTrue();
        assertThat(app.getDocumentPassedAt()).isEqualTo(PASSED_AT);
    }

    @Test
    void 면접_후_합격해도_서류_통과_기록은_유지된다() {
        Application app = applicationInInterview();

        app.advanceStatus(ApplicationStatus.ACCEPTED);

        assertThat(app.getDocumentPassedAt()).isEqualTo(PASSED_AT);
    }

    @Test
    void 서류_단계에서_불합격하면_서류_통과로_기록되지_않는다() {
        Application app = applicationInResumeSubmitted();

        app.advanceStatus(ApplicationStatus.REJECTED);

        assertThat(app.isDocumentPassed()).isFalse();
        assertThat(app.getDocumentPassedAt()).isNull();
    }

    @Test
    void 면접_일정이_또_잡혀도_최초_서류_통과_시점을_덮어쓰지_않는다() {
        Application app = applicationInInterview();

        app.startInterview(PASSED_AT.plusDays(3));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(app.getDocumentPassedAt()).isEqualTo(PASSED_AT);
    }

    @Test
    void 이미_불합격한_지원에_면접_일정이_잡혀도_상태와_기록은_바뀌지_않는다() {
        Application app = applicationInResumeSubmitted();
        app.advanceStatus(ApplicationStatus.REJECTED);

        app.startInterview(PASSED_AT);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(app.isDocumentPassed()).isFalse();
    }

    @Test
    void 면접으로는_상태_전이로_직접_바꿀_수_없고_면접_일정을_등록해야_한다() {
        Application app = applicationInResumeSubmitted();

        assertErrorCode(() -> app.advanceStatus(ApplicationStatus.INTERVIEW), ErrorCode.INTERVIEW_SCHEDULE_REQUIRED);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
        assertThat(app.isDocumentPassed()).isFalse();
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

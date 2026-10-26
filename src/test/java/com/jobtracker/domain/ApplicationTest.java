package com.jobtracker.domain;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTest {

    private Company company() {
        return Company.create(1L, "카카오", "IT", "https://kakao.com");
    }

    @Test
    void create_초기_상태는_RESUME_SUBMITTED이다() {
        Application app = Application.create(1L, company(), "백엔드 개발자", LocalDate.now());

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void advanceStatus_서류합격으로_전이한다() {
        Application app = Application.create(1L, company(), "백엔드 개발자", LocalDate.now());
        app.advanceStatus(ApplicationStatus.RESUME_PASSED);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_PASSED);
    }

    @Test
    void advanceStatus_유효하지_않은_전이시_예외를_던진다() {
        Application app = Application.create(1L, company(), "백엔드 개발자", LocalDate.now());

        assertThatThrownBy(() -> app.advanceStatus(ApplicationStatus.ACCEPTED))
                .isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest
    @MethodSource("allInProgressStatuses")
    void advanceStatus_불합격은_모든_진행_상태에서_가능하다(ApplicationStatus fromStatus) {
        Application app = Application.create(1L, company(), "백엔드 개발자", LocalDate.now());

        // advance to the target status first
        switch (fromStatus) {
            case RESUME_SUBMITTED:
                // already initial state, no advance needed
                break;
            case RESUME_PASSED:
                app.advanceStatus(ApplicationStatus.RESUME_PASSED);
                break;
            case INTERVIEW_1:
                app.advanceStatus(ApplicationStatus.RESUME_PASSED);
                app.advanceStatus(ApplicationStatus.INTERVIEW_1);
                break;
            case INTERVIEW_2:
                app.advanceStatus(ApplicationStatus.RESUME_PASSED);
                app.advanceStatus(ApplicationStatus.INTERVIEW_1);
                app.advanceStatus(ApplicationStatus.INTERVIEW_2);
                break;
            case FINAL:
                app.advanceStatus(ApplicationStatus.RESUME_PASSED);
                app.advanceStatus(ApplicationStatus.INTERVIEW_1);
                app.advanceStatus(ApplicationStatus.FINAL);
                break;
            default:
                throw new IllegalArgumentException("Unexpected status: " + fromStatus);
        }

        app.advanceStatus(ApplicationStatus.REJECTED);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    static Stream<ApplicationStatus> allInProgressStatuses() {
        return Stream.of(
            ApplicationStatus.RESUME_SUBMITTED,
            ApplicationStatus.RESUME_PASSED,
            ApplicationStatus.INTERVIEW_1,
            ApplicationStatus.INTERVIEW_2,
            ApplicationStatus.FINAL
        );
    }
}

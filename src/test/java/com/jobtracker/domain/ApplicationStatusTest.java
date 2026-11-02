package com.jobtracker.domain;

import com.jobtracker.domain.application.ApplicationStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
}

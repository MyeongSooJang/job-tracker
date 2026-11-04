package com.jobtracker.domain;

import com.jobtracker.domain.studylog.StudyPeriod;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class StudyPeriodTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

    @Test
    void 시작일과_종료일이_같으면_하루_구간이다() {
        StudyPeriod period = StudyPeriod.of(FROM, FROM, 62);

        assertThat(period.from()).isEqualTo(FROM);
        assertThat(period.to()).isEqualTo(FROM);
    }

    @Test
    void 상한과_같은_일수의_구간은_허용한다() {
        LocalDate to = FROM.plusDays(61);

        assertThat(StudyPeriod.of(FROM, to, 62).to()).isEqualTo(to);
    }

    @Test
    void 상한을_하루라도_넘으면_거부한다() {
        assertErrorCode(() -> StudyPeriod.of(FROM, FROM.plusDays(62), 62), ErrorCode.INVALID_STUDY_RANGE);
    }

    @Test
    void 시작일이_종료일보다_늦으면_거부한다() {
        assertErrorCode(() -> StudyPeriod.of(FROM.plusDays(1), FROM, 62), ErrorCode.INVALID_STUDY_RANGE);
    }

    @Test
    void 시작일이나_종료일이_없으면_거부한다() {
        assertErrorCode(() -> StudyPeriod.of(null, FROM, 62), ErrorCode.INVALID_STUDY_RANGE);
        assertErrorCode(() -> StudyPeriod.of(FROM, null, 62), ErrorCode.INVALID_STUDY_RANGE);
    }
}

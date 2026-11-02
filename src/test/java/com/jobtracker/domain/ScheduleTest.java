package com.jobtracker.domain;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleType;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ScheduleTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 11, 10, 14, 0);

    private Schedule interviewSchedule() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        Application application = Application.create(OWNER_ID, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
        return Schedule.create(application, "1차 면접", SCHEDULED_AT, ScheduleType.INTERVIEW, "판교", "정장");
    }

    @Test
    void 일정을_만들면_입력한_정보가_그대로_담긴다() {
        Schedule schedule = interviewSchedule();

        assertThat(schedule.getTitle()).isEqualTo("1차 면접");
        assertThat(schedule.getScheduledAt()).isEqualTo(SCHEDULED_AT);
        assertThat(schedule.getType()).isEqualTo(ScheduleType.INTERVIEW);
        assertThat(schedule.getAddress()).isEqualTo("판교");
        assertThat(schedule.getNote()).isEqualTo("정장");
    }

    @Test
    void 주소를_변경하면_새_주소로_바뀐다() {
        Schedule schedule = interviewSchedule();

        schedule.changeAddress("강남");

        assertThat(schedule.getAddress()).isEqualTo("강남");
    }

    @Test
    void 소유자는_소유권_검증을_통과한다() {
        Schedule schedule = interviewSchedule();

        assertThatCode(() -> schedule.verifyOwner(OWNER_ID)).doesNotThrowAnyException();
    }

    @Test
    void 다른_사용자의_일정은_존재하지_않는_것으로_취급한다() {
        Schedule schedule = interviewSchedule();

        assertErrorCode(() -> schedule.verifyOwner(OTHER_USER_ID), ErrorCode.SCHEDULE_NOT_FOUND);
    }
}

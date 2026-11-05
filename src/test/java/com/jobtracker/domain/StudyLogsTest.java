package com.jobtracker.domain;

import com.jobtracker.domain.studylog.DailyStudySummary;
import com.jobtracker.domain.studylog.StudyLog;
import com.jobtracker.domain.studylog.StudyLogs;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudyLogsTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    private StudyLog log(int daysAgo, int minutes) {
        return StudyLog.create(USER_ID, TODAY.minusDays(daysAgo), "제목", "공부", minutes, TODAY);
    }

    @Test
    void 같은_날_여러_글의_공부_시간은_합산한다() {
        StudyLogs logs = new StudyLogs(List.of(log(0, 60), log(0, 30), log(1, 45)));

        assertThat(logs.minutesOn(TODAY)).isEqualTo(90);
        assertThat(logs.minutesOn(TODAY.minusDays(1))).isEqualTo(45);
    }

    @Test
    void 글이_없는_날의_공부_시간은_0이다() {
        assertThat(new StudyLogs(List.of()).minutesOn(TODAY)).isEqualTo(0);
    }

    @Test
    void 날짜별로_글_수와_공부_시간_합계를_오래된_날짜부터_요약한다() {
        StudyLogs logs = new StudyLogs(List.of(log(0, 60), log(2, 20), log(0, 30)));

        assertThat(logs.dailySummaries()).containsExactly(
                new DailyStudySummary(TODAY.minusDays(2), 1, 20),
                new DailyStudySummary(TODAY, 2, 90));
    }

    @Test
    void 공부_시간이_0분인_글도_글_수에는_포함한다() {
        StudyLogs logs = new StudyLogs(List.of(log(0, 0)));

        assertThat(logs.dailySummaries()).containsExactly(new DailyStudySummary(TODAY, 1, 0));
    }

    @Test
    void 글이_없으면_요약도_비어_있다() {
        assertThat(new StudyLogs(List.of()).dailySummaries()).isEmpty();
    }
}

package com.jobtracker.domain;

import com.jobtracker.domain.studylog.DailyStudyMinutes;
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
        return StudyLog.create(USER_ID, TODAY.minusDays(daysAgo), "공부", minutes, TODAY);
    }

    @Test
    void 구간의_모든_날짜를_오래된_날짜부터_돌려준다() {
        StudyLogs logs = new StudyLogs(List.of());

        List<DailyStudyMinutes> result = logs.minutesByDay(TODAY.minusDays(6), TODAY);

        assertThat(result).hasSize(7);
        assertThat(result.get(0).date()).isEqualTo(TODAY.minusDays(6));
        assertThat(result.get(6).date()).isEqualTo(TODAY);
    }

    @Test
    void 기록이_없는_날은_0분으로_채운다() {
        StudyLogs logs = new StudyLogs(List.of(log(0, 90), log(2, 30)));

        List<DailyStudyMinutes> result = logs.minutesByDay(TODAY.minusDays(2), TODAY);

        assertThat(result).extracting(DailyStudyMinutes::minutes).containsExactly(30, 0, 90);
    }

    @Test
    void 구간_밖의_기록은_포함하지_않는다() {
        StudyLogs logs = new StudyLogs(List.of(log(10, 200)));

        List<DailyStudyMinutes> result = logs.minutesByDay(TODAY.minusDays(1), TODAY);

        assertThat(result).extracting(DailyStudyMinutes::minutes).containsExactly(0, 0);
    }

    @Test
    void 같은_날_여러_글의_공부_시간은_합산한다() {
        StudyLogs logs = new StudyLogs(List.of(log(0, 60), log(0, 30), log(1, 45)));

        assertThat(logs.minutesOn(TODAY)).isEqualTo(90);
        assertThat(logs.minutesByDay(TODAY.minusDays(1), TODAY))
                .extracting(DailyStudyMinutes::minutes).containsExactly(45, 90);
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

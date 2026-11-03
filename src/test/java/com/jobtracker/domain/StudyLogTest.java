package com.jobtracker.domain;

import com.jobtracker.domain.studylog.StudyLog;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class StudyLogTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    private StudyLog studyLog() {
        return StudyLog.create(USER_ID, TODAY, "JPA 연관관계 공부", 90, TODAY);
    }

    @Test
    void 오늘_날짜로_기록하면_내용과_공부_시간이_저장된다() {
        StudyLog studyLog = studyLog();

        assertThat(studyLog.getStudyDate()).isEqualTo(TODAY);
        assertThat(studyLog.getContent()).isEqualTo("JPA 연관관계 공부");
        assertThat(studyLog.getStudyMinutes()).isEqualTo(90);
    }

    @Test
    void 과거_날짜로도_기록할_수_있다() {
        LocalDate yesterday = TODAY.minusDays(1);

        StudyLog studyLog = StudyLog.create(USER_ID, yesterday, "알고리즘", 30, TODAY);

        assertThat(studyLog.getStudyDate()).isEqualTo(yesterday);
    }

    @Test
    void 미래_날짜로는_기록할_수_없다() {
        assertErrorCode(
                () -> StudyLog.create(USER_ID, TODAY.plusDays(1), "미리 쓴 기록", 30, TODAY),
                ErrorCode.FUTURE_STUDY_DATE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void 내용이_비어_있으면_기록할_수_없다(String content) {
        assertErrorCode(
                () -> StudyLog.create(USER_ID, TODAY, content, 30, TODAY),
                ErrorCode.STUDY_CONTENT_REQUIRED);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1441})
    void 공부_시간이_0에서_1440분_범위를_벗어나면_기록할_수_없다(int minutes) {
        assertErrorCode(
                () -> StudyLog.create(USER_ID, TODAY, "JPA", minutes, TODAY),
                ErrorCode.INVALID_STUDY_MINUTES);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1440})
    void 공부_시간의_경계값은_허용한다(int minutes) {
        StudyLog studyLog = StudyLog.create(USER_ID, TODAY, "JPA", minutes, TODAY);

        assertThat(studyLog.getStudyMinutes()).isEqualTo(minutes);
    }

    @Test
    void 다시_쓰면_내용과_공부_시간이_바뀐다() {
        StudyLog studyLog = studyLog();

        studyLog.write("JPA 영속성 컨텍스트 추가 정리", 120);

        assertThat(studyLog.getContent()).isEqualTo("JPA 영속성 컨텍스트 추가 정리");
        assertThat(studyLog.getStudyMinutes()).isEqualTo(120);
    }

    @Test
    void 다시_쓸_때_내용이_비어_있으면_기존_기록이_유지된다() {
        StudyLog studyLog = studyLog();

        assertErrorCode(() -> studyLog.write(" ", 120), ErrorCode.STUDY_CONTENT_REQUIRED);

        assertThat(studyLog.getContent()).isEqualTo("JPA 연관관계 공부");
        assertThat(studyLog.getStudyMinutes()).isEqualTo(90);
    }

    @Test
    void 기록한_날짜의_공부_시간만_알려준다() {
        StudyLog studyLog = studyLog();

        assertThat(studyLog.minutesOn(TODAY)).isEqualTo(90);
        assertThat(studyLog.minutesOn(TODAY.minusDays(1))).isZero();
    }
}

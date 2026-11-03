package com.jobtracker.application;

import com.jobtracker.application.service.StudyLogService;
import com.jobtracker.application.service.dto.StudyLogRequest;
import com.jobtracker.application.service.dto.StudyLogResponse;
import com.jobtracker.application.service.dto.StudyStatsResponse;
import com.jobtracker.domain.studylog.DailyStudyMinutes;
import com.jobtracker.domain.studylog.StudyLog;
import com.jobtracker.domain.studylog.StudyLogRepository;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudyLogServiceTest {

    private static final Long USER_ID = 1L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @Mock StudyLogRepository studyLogRepository;
    StudyLogService studyLogService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-03T03:00:00Z"), SEOUL);
        studyLogService = new StudyLogService(studyLogRepository, clock);
    }

    private StudyLog log(LocalDate date, int minutes) {
        return StudyLog.create(USER_ID, date, "공부 내용", minutes, TODAY);
    }

    @Test
    void 기록이_없는_날짜에_저장하면_새_기록이_만들어진다() {
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, TODAY)).willReturn(Optional.empty());
        given(studyLogRepository.save(any(StudyLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        StudyLogResponse response = studyLogService.save(USER_ID, TODAY, new StudyLogRequest("JPA 공부", 90));

        assertThat(response.studyDate()).isEqualTo(TODAY);
        assertThat(response.content()).isEqualTo("JPA 공부");
        assertThat(response.studyMinutes()).isEqualTo(90);
    }

    @Test
    void 공부_시간을_생략하면_0분으로_저장한다() {
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, TODAY)).willReturn(Optional.empty());
        given(studyLogRepository.save(any(StudyLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        StudyLogResponse response = studyLogService.save(USER_ID, TODAY, new StudyLogRequest("JPA 공부", null));

        assertThat(response.studyMinutes()).isZero();
    }

    @Test
    void 기록이_있는_날짜에_저장하면_새로_만들지_않고_기존_기록을_수정한다() {
        StudyLog existing = log(TODAY, 30);
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, TODAY)).willReturn(Optional.of(existing));

        StudyLogResponse response = studyLogService.save(USER_ID, TODAY, new StudyLogRequest("내용 보강", 120));

        assertThat(response.content()).isEqualTo("내용 보강");
        assertThat(existing.getContent()).isEqualTo("내용 보강");
        assertThat(existing.getStudyMinutes()).isEqualTo(120);
        verify(studyLogRepository, never()).save(any());
    }

    @Test
    void 오늘_이후_날짜에는_저장할_수_없다() {
        LocalDate tomorrow = TODAY.plusDays(1);
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, tomorrow)).willReturn(Optional.empty());

        assertErrorCode(
                () -> studyLogService.save(USER_ID, tomorrow, new StudyLogRequest("미리 쓴 기록", 30)),
                ErrorCode.FUTURE_STUDY_DATE);
        verify(studyLogRepository, never()).save(any());
    }

    @Test
    void 기록이_없는_날짜는_조회할_수_없다() {
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, TODAY)).willReturn(Optional.empty());

        assertErrorCode(() -> studyLogService.find(USER_ID, TODAY), ErrorCode.STUDY_LOG_NOT_FOUND);
    }

    @Test
    void 기록이_있는_날짜는_삭제한다() {
        StudyLog existing = log(TODAY, 30);
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, TODAY)).willReturn(Optional.of(existing));

        studyLogService.delete(USER_ID, TODAY);

        verify(studyLogRepository).delete(existing);
    }

    @Test
    void 기록이_없는_날짜는_삭제할_수_없다() {
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, TODAY)).willReturn(Optional.empty());

        assertErrorCode(() -> studyLogService.delete(USER_ID, TODAY), ErrorCode.STUDY_LOG_NOT_FOUND);
        verify(studyLogRepository, never()).delete(any());
    }

    @Test
    void 월별_조회는_그_달_1일부터_말일까지의_기록을_돌려준다() {
        given(studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(
                USER_ID, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .willReturn(List.of(log(LocalDate.of(2026, 11, 1), 30), log(TODAY, 60)));

        List<StudyLogResponse> responses = studyLogService.findByMonth(USER_ID, YearMonth.of(2026, 11));

        assertThat(responses).extracting(StudyLogResponse::studyMinutes).containsExactly(30, 60);
    }

    @Test
    void 통계는_연속_작성일과_이번_달_작성_일수와_최근_7일_공부_시간을_돌려준다() {
        LocalDate yesterday = TODAY.minusDays(1);
        LocalDate weekStart = TODAY.minusDays(6);
        given(studyLogRepository.findStudyDatesByUserId(USER_ID)).willReturn(List.of(TODAY, yesterday));
        given(studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(USER_ID, weekStart, TODAY))
                .willReturn(List.of(log(yesterday, 30), log(TODAY, 90)));
        given(studyLogRepository.countByUserIdAndStudyDateBetween(
                USER_ID, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).willReturn(2L);

        StudyStatsResponse stats = studyLogService.stats(USER_ID);

        assertThat(stats.currentStreak()).isEqualTo(2);
        assertThat(stats.monthWrittenDays()).isEqualTo(2);
        assertThat(stats.last7Days()).hasSize(7);
        assertThat(stats.last7Days()).extracting(DailyStudyMinutes::minutes)
                .containsExactly(0, 0, 0, 0, 0, 30, 90);
    }
}

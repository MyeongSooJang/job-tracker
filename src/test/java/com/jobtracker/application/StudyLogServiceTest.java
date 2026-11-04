package com.jobtracker.application;

import com.jobtracker.application.service.StudyLogService;
import com.jobtracker.application.service.dto.HeatmapDayResponse;
import com.jobtracker.application.service.dto.StudyLogCreateRequest;
import com.jobtracker.application.service.dto.StudyLogRequest;
import com.jobtracker.application.service.dto.StudyLogResponse;
import com.jobtracker.application.service.dto.StudyStatsResponse;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudyLogServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long LOG_ID = 10L;
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
        return StudyLog.create(USER_ID, date, "제목", "공부 내용", minutes, TODAY);
    }

    @Test
    void 글을_작성하면_새_기록이_만들어진다() {
        given(studyLogRepository.save(any(StudyLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        StudyLogResponse response = studyLogService.create(USER_ID,
                new StudyLogCreateRequest(TODAY, "JPA 정리", "JPA 공부", 90));

        assertThat(response.studyDate()).isEqualTo(TODAY);
        assertThat(response.title()).isEqualTo("JPA 정리");
        assertThat(response.content()).isEqualTo("JPA 공부");
        assertThat(response.studyMinutes()).isEqualTo(90);
    }

    @Test
    void 공부_시간을_생략하면_0분으로_저장한다() {
        given(studyLogRepository.save(any(StudyLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        StudyLogResponse response = studyLogService.create(USER_ID,
                new StudyLogCreateRequest(TODAY, "JPA 정리", "JPA 공부", null));

        assertThat(response.studyMinutes()).isZero();
    }

    @Test
    void 같은_날짜에_글을_또_작성해도_기존_글을_건드리지_않고_새로_저장한다() {
        given(studyLogRepository.save(any(StudyLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        studyLogService.create(USER_ID, new StudyLogCreateRequest(TODAY, "아침", "내용1", 30));
        studyLogService.create(USER_ID, new StudyLogCreateRequest(TODAY, "저녁", "내용2", 30));

        verify(studyLogRepository, times(2)).save(any(StudyLog.class));
    }

    @Test
    void 오늘_이후_날짜에는_작성할_수_없다() {
        assertErrorCode(
                () -> studyLogService.create(USER_ID,
                        new StudyLogCreateRequest(TODAY.plusDays(1), "미리", "미리 쓴 기록", 30)),
                ErrorCode.FUTURE_STUDY_DATE);
        verify(studyLogRepository, never()).save(any());
    }

    @Test
    void 내_글을_수정하면_제목과_내용과_공부_시간이_바뀐다() {
        StudyLog existing = log(TODAY, 30);
        given(studyLogRepository.findByIdAndUserId(LOG_ID, USER_ID)).willReturn(Optional.of(existing));

        StudyLogResponse response = studyLogService.update(USER_ID, LOG_ID, new StudyLogRequest("새 제목", "내용 보강", 120));

        assertThat(response.title()).isEqualTo("새 제목");
        assertThat(existing.getContent()).isEqualTo("내용 보강");
        assertThat(existing.getStudyMinutes()).isEqualTo(120);
        verify(studyLogRepository, never()).save(any());
    }

    @Test
    void 다른_사용자의_글은_수정할_수_없다() {
        given(studyLogRepository.findByIdAndUserId(LOG_ID, USER_ID)).willReturn(Optional.empty());

        assertErrorCode(
                () -> studyLogService.update(USER_ID, LOG_ID, new StudyLogRequest("제목", "내용", 30)),
                ErrorCode.STUDY_LOG_NOT_FOUND);
    }

    @Test
    void 없는_글은_조회할_수_없다() {
        given(studyLogRepository.findByIdAndUserId(LOG_ID, USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> studyLogService.find(USER_ID, LOG_ID), ErrorCode.STUDY_LOG_NOT_FOUND);
    }

    @Test
    void 내_글은_삭제한다() {
        StudyLog existing = log(TODAY, 30);
        given(studyLogRepository.findByIdAndUserId(LOG_ID, USER_ID)).willReturn(Optional.of(existing));

        studyLogService.delete(USER_ID, LOG_ID);

        verify(studyLogRepository).delete(existing);
    }

    @Test
    void 없는_글은_삭제할_수_없다() {
        given(studyLogRepository.findByIdAndUserId(LOG_ID, USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> studyLogService.delete(USER_ID, LOG_ID), ErrorCode.STUDY_LOG_NOT_FOUND);
        verify(studyLogRepository, never()).delete(any());
    }

    @Test
    void 구간_조회는_저장소가_돌려준_순서대로_글을_돌려준다() {
        LocalDate from = TODAY.minusDays(13);
        given(studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(USER_ID, from, TODAY))
                .willReturn(List.of(log(TODAY, 60), log(from, 30)));

        List<StudyLogResponse> responses = studyLogService.findRange(USER_ID, from, TODAY);

        assertThat(responses).extracting(StudyLogResponse::studyMinutes).containsExactly(60, 30);
    }

    @Test
    void 구간_조회는_62일을_넘으면_거부한다() {
        assertErrorCode(
                () -> studyLogService.findRange(USER_ID, TODAY.minusDays(62), TODAY),
                ErrorCode.INVALID_STUDY_RANGE);
    }

    @Test
    void 구간_조회는_62일까지_허용한다() {
        LocalDate from = TODAY.minusDays(61);
        given(studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(USER_ID, from, TODAY))
                .willReturn(List.of());

        assertThat(studyLogService.findRange(USER_ID, from, TODAY)).isEmpty();
    }

    @Test
    void 통계는_현재_연속과_최장_연속과_이번_달_작성_일수와_오늘_공부_시간을_돌려준다() {
        LocalDate yesterday = TODAY.minusDays(1);
        // 최근 2일 연속(오늘 포함), 과거에 4일 연속
        given(studyLogRepository.findStudyDatesByUserId(USER_ID)).willReturn(List.of(
                TODAY, yesterday,
                TODAY.minusDays(10), TODAY.minusDays(11), TODAY.minusDays(12), TODAY.minusDays(13)));
        given(studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(USER_ID, TODAY, TODAY))
                .willReturn(List.of(log(TODAY, 30), log(TODAY, 60)));
        given(studyLogRepository.countStudyDatesBetween(
                USER_ID, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).willReturn(2L);

        StudyStatsResponse stats = studyLogService.stats(USER_ID);

        assertThat(stats.currentStreak()).isEqualTo(2);
        assertThat(stats.longestStreak()).isEqualTo(4);
        assertThat(stats.monthWrittenDays()).isEqualTo(2);
        assertThat(stats.todayMinutes()).isEqualTo(90);
    }

    @Test
    void 오늘_쓴_글이_없으면_오늘_공부_시간은_0이다() {
        given(studyLogRepository.findStudyDatesByUserId(USER_ID)).willReturn(List.of());
        given(studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(USER_ID, TODAY, TODAY))
                .willReturn(List.of());
        given(studyLogRepository.countStudyDatesBetween(
                USER_ID, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).willReturn(0L);

        StudyStatsResponse stats = studyLogService.stats(USER_ID);

        assertThat(stats.todayMinutes()).isZero();
        assertThat(stats.currentStreak()).isZero();
        assertThat(stats.longestStreak()).isZero();
    }

    @Test
    void 잔디는_글이_있는_날만_글_수와_공부_시간_합계로_돌려준다() {
        LocalDate from = TODAY.minusDays(30);
        LocalDate twoDaysAgo = TODAY.minusDays(2);
        given(studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(USER_ID, from, TODAY))
                .willReturn(List.of(log(TODAY, 60), log(TODAY, 30), log(twoDaysAgo, 20)));

        List<HeatmapDayResponse> heatmap = studyLogService.heatmap(USER_ID, from, TODAY);

        assertThat(heatmap).containsExactly(
                new HeatmapDayResponse(twoDaysAgo, 1, 20),
                new HeatmapDayResponse(TODAY, 2, 90));
    }

    @Test
    void 잔디_조회는_400일을_넘으면_거부한다() {
        assertErrorCode(
                () -> studyLogService.heatmap(USER_ID, TODAY.minusDays(400), TODAY),
                ErrorCode.INVALID_STUDY_RANGE);
    }

    @Test
    void 잔디_조회는_400일까지_허용한다() {
        LocalDate from = TODAY.minusDays(399);
        given(studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(USER_ID, from, TODAY))
                .willReturn(List.of());

        assertThat(studyLogService.heatmap(USER_ID, from, TODAY)).isEmpty();
    }
}

package com.jobtracker.domain;

import com.jobtracker.domain.studylog.StudyStreak;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudyStreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    private StudyStreak streakOf(int... daysAgo) {
        return StudyStreak.of(java.util.Arrays.stream(daysAgo).mapToObj(TODAY::minusDays).toList());
    }

    @Test
    void 기록이_없으면_연속_작성일은_0이다() {
        assertThat(StudyStreak.of(List.of()).current(TODAY)).isZero();
    }

    @Test
    void 오늘까지_이어서_썼으면_오늘을_포함해_센다() {
        assertThat(streakOf(0, 1, 2).current(TODAY)).isEqualTo(3);
    }

    @Test
    void 오늘은_아직_안_썼어도_어제까지_이어졌으면_끊기지_않는다() {
        assertThat(streakOf(1, 2, 3, 4).current(TODAY)).isEqualTo(4);
    }

    @Test
    void 어제도_오늘도_안_썼으면_0이다() {
        assertThat(streakOf(2, 3, 4).current(TODAY)).isZero();
    }

    @Test
    void 중간에_하루라도_비면_거기서_끊긴다() {
        assertThat(streakOf(0, 1, 3, 4).current(TODAY)).isEqualTo(2);
    }

    @Test
    void 월을_넘어서도_이어서_센다() {
        StudyStreak streak = StudyStreak.of(List.of(
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 30)));

        assertThat(streak.current(TODAY)).isEqualTo(4);
    }
}

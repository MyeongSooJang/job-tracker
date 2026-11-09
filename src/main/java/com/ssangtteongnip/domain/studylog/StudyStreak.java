package com.ssangtteongnip.domain.studylog;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

public class StudyStreak {

    private final Set<LocalDate> writtenDates;

    private StudyStreak(Set<LocalDate> writtenDates) {
        this.writtenDates = writtenDates;
    }

    public static StudyStreak of(Collection<LocalDate> writtenDates) {
        return new StudyStreak(Set.copyOf(writtenDates));
    }

    // 오늘 아직 쓰지 않았어도 어제까지 이어졌다면 끊긴 것으로 보지 않는다.
    public int current(LocalDate today) {
        LocalDate day = writtenDates.contains(today) ? today : today.minusDays(1);
        int days = 0;
        while (writtenDates.contains(day)) {
            days++;
            day = day.minusDays(1);
        }
        return days;
    }

    // 연속 구간의 시작일(바로 전날이 기록 없는 날)에서만 세기 시작해 모든 날짜를 한 번씩만 지난다.
    public int longest() {
        int longest = 0;
        for (LocalDate date : writtenDates) {
            if (writtenDates.contains(date.minusDays(1))) {
                continue;
            }
            int length = 1;
            LocalDate next = date.plusDays(1);
            while (writtenDates.contains(next)) {
                length++;
                next = next.plusDays(1);
            }
            longest = Math.max(longest, length);
        }
        return longest;
    }
}

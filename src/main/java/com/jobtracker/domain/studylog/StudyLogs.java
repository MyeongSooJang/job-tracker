package com.jobtracker.domain.studylog;

import java.time.LocalDate;
import java.util.List;

public class StudyLogs {

    private final List<StudyLog> logs;

    public StudyLogs(List<StudyLog> logs) {
        this.logs = List.copyOf(logs);
    }

    // 기록이 없는 날도 0분으로 채워, 구간의 모든 날짜를 오래된 날짜부터 돌려준다.
    public List<DailyStudyMinutes> minutesByDay(LocalDate from, LocalDate to) {
        return from.datesUntil(to.plusDays(1))
                .map(day -> new DailyStudyMinutes(day, minutesOn(day)))
                .toList();
    }

    private int minutesOn(LocalDate day) {
        return logs.stream().mapToInt(log -> log.minutesOn(day)).sum();
    }
}

package com.jobtracker.domain.studylog;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class StudyLogs {

    private final List<StudyLog> logs;

    public StudyLogs(List<StudyLog> logs) {
        this.logs = List.copyOf(logs);
    }

    // 같은 날 여러 글의 공부 시간은 합산한다.
    public int minutesOn(LocalDate day) {
        return logs.stream().mapToInt(log -> log.minutesOn(day)).sum();
    }

    // 글이 있는 날만, 오래된 날짜부터 글 수와 공부 시간 합계를 요약한다.
    public List<DailyStudySummary> dailySummaries() {
        return logs.stream()
                .map(StudyLog::getStudyDate)
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .map(day -> new DailyStudySummary(day, countOn(day), minutesOn(day)))
                .toList();
    }

    private int countOn(LocalDate day) {
        return (int) logs.stream().filter(log -> log.isOn(day)).count();
    }
}

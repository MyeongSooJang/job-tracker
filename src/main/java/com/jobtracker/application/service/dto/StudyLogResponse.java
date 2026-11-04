package com.jobtracker.application.service.dto;

import com.jobtracker.domain.studylog.StudyLog;

import java.time.LocalDate;

public record StudyLogResponse(Long id, LocalDate studyDate, String title, String content, int studyMinutes) {

    public static StudyLogResponse from(StudyLog studyLog) {
        return new StudyLogResponse(studyLog.getId(), studyLog.getStudyDate(), studyLog.getTitle(),
                studyLog.getContent(), studyLog.getStudyMinutes());
    }
}

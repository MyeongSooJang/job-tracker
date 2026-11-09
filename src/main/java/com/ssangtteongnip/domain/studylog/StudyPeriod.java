package com.ssangtteongnip.domain.studylog;

import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record StudyPeriod(LocalDate from, LocalDate to) {

    public static StudyPeriod of(LocalDate from, LocalDate to, int maxDays) {
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > maxDays) {
            throw new BusinessException(ErrorCode.INVALID_STUDY_RANGE);
        }
        return new StudyPeriod(from, to);
    }
}

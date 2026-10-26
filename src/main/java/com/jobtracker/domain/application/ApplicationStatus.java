package com.jobtracker.domain.application;

public enum ApplicationStatus {
    RESUME_SUBMITTED,
    RESUME_PASSED,
    INTERVIEW_1,
    INTERVIEW_2,
    FINAL,
    ACCEPTED,
    REJECTED;

    public boolean canAdvanceTo(ApplicationStatus next) {
        return switch (this) {
            case RESUME_SUBMITTED -> next == RESUME_PASSED || next == REJECTED;
            case RESUME_PASSED    -> next == INTERVIEW_1 || next == REJECTED;
            case INTERVIEW_1      -> next == INTERVIEW_2 || next == FINAL || next == REJECTED;
            case INTERVIEW_2      -> next == FINAL || next == REJECTED;
            case FINAL            -> next == ACCEPTED || next == REJECTED;
            case ACCEPTED, REJECTED -> false;
        };
    }
}

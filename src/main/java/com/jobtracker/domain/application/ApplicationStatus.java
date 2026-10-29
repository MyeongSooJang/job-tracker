package com.jobtracker.domain.application;

public enum ApplicationStatus {
    RESUME_SUBMITTED,
    INTERVIEW,
    ACCEPTED,
    REJECTED;

    public boolean canAdvanceTo(ApplicationStatus next) {
        return switch (this) {
            case RESUME_SUBMITTED -> next == INTERVIEW || next == REJECTED;
            case INTERVIEW        -> next == ACCEPTED || next == REJECTED;
            case ACCEPTED, REJECTED -> false;
        };
    }
}

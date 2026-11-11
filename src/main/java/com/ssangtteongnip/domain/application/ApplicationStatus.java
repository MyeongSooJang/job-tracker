package com.ssangtteongnip.domain.application;

import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;

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

    // 사용자가 요청한 결과(this)를 지원 건에 적용한다. 합격/불합격만 직접 요청할 수 있고,
    // 면접은 면접 일정을 등록해야 하며 서류 제출은 되돌아갈 수 없는 시작 상태다.
    public void applyTo(Application application) {
        switch (this) {
            case ACCEPTED -> application.accept();
            case REJECTED -> application.reject();
            case INTERVIEW -> throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_REQUIRED);
            case RESUME_SUBMITTED -> throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}

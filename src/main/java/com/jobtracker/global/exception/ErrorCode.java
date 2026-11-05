package com.jobtracker.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "지원 건을 찾을 수 없습니다."),
    INTERVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "면접을 찾을 수 없습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."),
    INTERVIEW_SCHEDULE_REQUIRED(HttpStatus.BAD_REQUEST, "면접 일정(일시, 장소)을 등록해야 면접 단계로 전환됩니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 전이입니다."),
    COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 회사입니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, "평점은 1~5 사이여야 합니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),
    RESUME_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 이력서가 없습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "업로드할 파일이 비어 있습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "PDF 파일만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기는 5MB 이하여야 합니다."),
    STORAGE_FAILED(HttpStatus.BAD_GATEWAY, "파일 저장소 처리 중 오류가 발생했습니다."),
    STUDY_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "학습 기록을 찾을 수 없습니다."),
    STUDY_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "공부한 내용을 입력해주세요."),
    INVALID_STUDY_MINUTES(HttpStatus.BAD_REQUEST, "공부 시간은 0~1440분 사이여야 합니다."),
    FUTURE_STUDY_DATE(HttpStatus.BAD_REQUEST, "미래 날짜에는 기록할 수 없습니다."),
    STUDY_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "제목을 1~100자로 입력해주세요."),
    INVALID_STUDY_RANGE(HttpStatus.BAD_REQUEST, "조회 기간이 올바르지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
    VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었거나 요청되지 않았습니다."),
    VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.BAD_REQUEST, "인증 코드를 5회 틀렸습니다. 새 코드를 요청해주세요."),
    VERIFICATION_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "인증 코드는 60초에 한 번만 요청할 수 있습니다."),
    MAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}

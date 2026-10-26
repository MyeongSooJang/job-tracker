package com.jobtracker.global.response;

import com.jobtracker.global.exception.ErrorCode;

public record ErrorResponse(String code, String message) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }
}

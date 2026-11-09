package com.ssangtteongnip.global.response;

import com.ssangtteongnip.global.exception.ErrorCode;

public record ErrorResponse(String code, String message) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }
}

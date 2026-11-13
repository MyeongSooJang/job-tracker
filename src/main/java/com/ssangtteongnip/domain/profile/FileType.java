package com.ssangtteongnip.domain.profile;

import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;

import java.util.Arrays;

public enum FileType {
    RESUME,
    PORTFOLIO,
    CERTIFICATE,
    ETC;

    public static FileType parse(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_FILE_CATEGORY));
    }
}

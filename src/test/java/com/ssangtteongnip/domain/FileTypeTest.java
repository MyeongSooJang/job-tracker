package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class FileTypeTest {

    @Test
    void 이름으로_종류를_찾는다() {
        assertThat(FileType.parse("PORTFOLIO")).isEqualTo(FileType.PORTFOLIO);
    }

    @Test
    void 알_수_없는_이름은_올바르지_않은_종류다() {
        assertErrorCode(() -> FileType.parse("DIPLOMA"), ErrorCode.INVALID_FILE_CATEGORY);
    }

    @Test
    void 이름이_없으면_올바르지_않은_종류다() {
        assertErrorCode(() -> FileType.parse(null), ErrorCode.INVALID_FILE_CATEGORY);
    }
}

package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-ssangtteongnip-0123456789-abcdef";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60);

    @Test
    void issue_발급한_토큰에서_사용자_ID를_꺼낼_수_있다() {
        String token = provider.issue(42L);

        assertThat(provider.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void parseUserId_만료된_토큰이면_예외가_발생한다() {
        JwtTokenProvider expired = new JwtTokenProvider(SECRET, -1);

        assertThatThrownBy(() -> provider.parseUserId(expired.issue(42L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void parseUserId_다른_키로_서명된_토큰이면_예외가_발생한다() {
        JwtTokenProvider other = new JwtTokenProvider("another-secret-key-for-ssangtteongnip-9876543210-zyx", 60);

        assertThatThrownBy(() -> provider.parseUserId(other.issue(42L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void parseUserId_페이로드를_바꾼_토큰이면_예외가_발생한다() {
        String[] parts = provider.issue(42L).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"1\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> provider.parseUserId(forged))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void parseUserId_형식이_잘못된_문자열이면_예외가_발생한다() {
        assertThatThrownBy(() -> provider.parseUserId("not-a-jwt"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}

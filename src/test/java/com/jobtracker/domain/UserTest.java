package com.jobtracker.domain;

import com.jobtracker.domain.user.User;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private final FakePasswordHasher hasher = new FakePasswordHasher();

    @Test
    void register_이메일은_공백제거_소문자로_정규화한다() {
        User user = User.register("  Hong@Example.COM ", "홍길동", "password1", hasher);

        assertThat(user.getEmail()).isEqualTo("hong@example.com");
        assertThat(user.getName()).isEqualTo("홍길동");
    }

    @Test
    void register_이메일_형식이_올바르지_않으면_예외가_발생한다() {
        assertThatThrownBy(() -> User.register("not-an-email", "홍길동", "password1", hasher))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL));
    }

    @Test
    void register_비밀번호가_8자_미만이면_예외가_발생한다() {
        assertThatThrownBy(() -> User.register("hong@example.com", "홍길동", "short12", hasher))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD));
    }

    @Test
    void authenticate_올바른_비밀번호면_예외가_발생하지_않는다() {
        User user = User.register("hong@example.com", "홍길동", "password1", hasher);

        assertThatCode(() -> user.authenticate("password1", hasher)).doesNotThrowAnyException();
    }

    @Test
    void authenticate_비밀번호가_틀리면_예외가_발생한다() {
        User user = User.register("hong@example.com", "홍길동", "password1", hasher);

        assertThatThrownBy(() -> user.authenticate("wrong-password", hasher))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}

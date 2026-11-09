package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.EmailVerificationService;
import com.ssangtteongnip.domain.FakeEmailVerificationStore;
import com.ssangtteongnip.domain.FakeMailSender;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL = "hong@example.com";
    private static final String CODE = "123456";
    private static final String WRONG_CODE = "000000";

    @Mock UserRepository userRepository;

    private final FakeEmailVerificationStore store = new FakeEmailVerificationStore();
    private final FakeMailSender mailSender = new FakeMailSender();
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(userRepository, store, mailSender, () -> CODE);
    }

    private void confirmWrongCode() {
        assertErrorCode(() -> service.confirm(EMAIL, WRONG_CODE), ErrorCode.INVALID_VERIFICATION_CODE);
    }

    @Test
    void 인증_코드를_요청하면_코드를_저장하고_메일로_발송한다() {
        service.send(EMAIL);

        assertThat(mailSender.lastCodeSentTo(EMAIL)).isEqualTo(CODE);
        assertThat(store.findCode(EMAIL)).isPresent();
    }

    @Test
    void 이메일은_공백을_제거하고_소문자로_정규화해서_발송한다() {
        service.send(" Hong@Example.com ");

        assertThat(mailSender.lastCodeSentTo(EMAIL)).isEqualTo(CODE);
    }

    @Test
    void 이메일_형식이_틀리면_발송하지_않는다() {
        assertErrorCode(() -> service.send("not-an-email"), ErrorCode.INVALID_EMAIL);

        assertThat(mailSender.sentCount()).isZero();
    }

    @Test
    void 이미_가입된_이메일이면_발송하지_않는다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertErrorCode(() -> service.send(EMAIL), ErrorCode.EMAIL_ALREADY_EXISTS);

        assertThat(mailSender.sentCount()).isZero();
        assertThat(store.findCode(EMAIL)).isEmpty();
    }

    @Test
    void 쿨다운_중에는_다시_발송할_수_없다() {
        service.send(EMAIL);

        assertErrorCode(() -> service.send(EMAIL), ErrorCode.VERIFICATION_TOO_FREQUENT);

        assertThat(mailSender.sentCount()).isEqualTo(1);
    }

    @Test
    void 메일_발송이_실패하면_코드와_쿨다운을_되돌려_바로_다시_요청할_수_있다() {
        mailSender.failFromNow();

        assertErrorCode(() -> service.send(EMAIL), ErrorCode.MAIL_SEND_FAILED);

        assertThat(store.findCode(EMAIL)).isEmpty();
        mailSender.recoverFromNow();
        service.send(EMAIL);
        assertThat(mailSender.sentCount()).isEqualTo(1);
    }

    @Test
    void 코드가_일치하면_인증_완료로_표시하고_코드를_지운다() {
        service.send(EMAIL);

        service.confirm(EMAIL, CODE);

        assertThat(store.isVerified(EMAIL)).isTrue();
        assertThat(store.findCode(EMAIL)).isEmpty();
    }

    @Test
    void 확인할_때도_이메일을_정규화한다() {
        service.send(EMAIL);

        service.confirm(" HONG@example.com ", CODE);

        assertThat(store.isVerified(EMAIL)).isTrue();
    }

    @Test
    void 요청된_코드가_없으면_만료로_처리한다() {
        assertErrorCode(() -> service.confirm(EMAIL, CODE), ErrorCode.VERIFICATION_EXPIRED);
    }

    @Test
    void 코드가_틀리면_인증되지_않고_코드는_유지된다() {
        service.send(EMAIL);

        confirmWrongCode();

        assertThat(store.isVerified(EMAIL)).isFalse();
        assertThat(store.findCode(EMAIL)).isPresent();
    }

    @Test
    void 네_번_틀린_뒤_맞으면_인증된다() {
        service.send(EMAIL);
        confirmWrongCode();
        confirmWrongCode();
        confirmWrongCode();
        confirmWrongCode();

        service.confirm(EMAIL, CODE);

        assertThat(store.isVerified(EMAIL)).isTrue();
    }

    @Test
    void 다섯_번째_오입력에서_코드를_폐기하고_이후에는_만료로_처리한다() {
        service.send(EMAIL);
        confirmWrongCode();
        confirmWrongCode();
        confirmWrongCode();
        confirmWrongCode();

        assertErrorCode(() -> service.confirm(EMAIL, WRONG_CODE), ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);

        assertThat(store.findCode(EMAIL)).isEmpty();
        assertErrorCode(() -> service.confirm(EMAIL, CODE), ErrorCode.VERIFICATION_EXPIRED);
        assertThat(store.isVerified(EMAIL)).isFalse();
    }

    // 동시 요청은 코드를 읽은 시점에는 모두 코드가 살아 있다. 다른 요청들이 시도 횟수를 이미 소진했다면
    // 뒤늦게 도착한 요청은 맞는 코드여도 통과하면 안 된다.
    @Test
    void 다른_요청이_시도_횟수를_이미_소진했다면_맞는_코드도_거절한다() {
        service.send(EMAIL);
        for (int i = 0; i < 5; i++) {
            store.recordAttempt(EMAIL);
        }

        assertErrorCode(() -> service.confirm(EMAIL, CODE), ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);

        assertThat(store.isVerified(EMAIL)).isFalse();
        assertThat(store.findCode(EMAIL)).isEmpty();
    }
}

package com.jobtracker.infrastructure;

import com.jobtracker.domain.user.VerificationCode;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.infrastructure.verification.SmtpMailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpMailSenderTest {

    @Mock JavaMailSender javaMailSender;

    private SmtpMailSender mailSender;

    @BeforeEach
    void setUp() {
        mailSender = new SmtpMailSender(javaMailSender, "noreply@example.com");
    }

    @Test
    void 인증_코드가_담긴_메일을_수신자에게_발송한다() {
        mailSender.sendVerificationCode("hong@example.com", VerificationCode.of("123456"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@example.com");
        assertThat(message.getTo()).containsExactly("hong@example.com");
        assertThat(message.getText()).contains("123456");
    }

    @Test
    void 발송에_실패하면_메일_발송_실패로_변환한다() {
        doThrow(new MailSendException("smtp down")).when(javaMailSender).send(any(SimpleMailMessage.class));

        assertErrorCode(
                () -> mailSender.sendVerificationCode("hong@example.com", VerificationCode.of("123456")),
                ErrorCode.MAIL_SEND_FAILED);
    }
}

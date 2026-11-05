package com.jobtracker.infrastructure.verification;

import com.jobtracker.domain.user.MailSender;
import com.jobtracker.domain.user.VerificationCode;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final String from;

    public SmtpMailSender(JavaMailSender javaMailSender, @Value("${app.mail.from}") String from) {
        this.javaMailSender = javaMailSender;
        this.from = from;
    }

    @Override
    public void sendVerificationCode(String email, VerificationCode code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[Job Tracker] 이메일 인증 코드");
        message.setText("인증 코드: " + code.value() + "\n5분 안에 입력해주세요.");
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            log.warn("인증 메일 발송 실패", e);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }
    }
}

package com.jobtracker.application.service;

import com.jobtracker.domain.user.CodeGenerator;
import com.jobtracker.domain.user.EmailVerificationStore;
import com.jobtracker.domain.user.MailSender;
import com.jobtracker.domain.user.User;
import com.jobtracker.domain.user.UserRepository;
import com.jobtracker.domain.user.VerificationCode;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 메일 발송(SMTP) 동안 DB 커넥션을 쥐지 않도록 클래스에 트랜잭션을 걸지 않는다.
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationStore store;
    private final MailSender mailSender;
    private final CodeGenerator codeGenerator;

    public void send(String rawEmail) {
        String email = User.requireValidEmail(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (!store.tryAcquireCooldown(email)) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOO_FREQUENT);
        }
        VerificationCode code = VerificationCode.issue(codeGenerator);
        store.saveCode(email, code);
        try {
            mailSender.sendVerificationCode(email, code);
        } catch (RuntimeException e) {
            store.deleteCode(email);
            store.releaseCooldown(email);
            throw e;
        }
    }

    public void confirm(String rawEmail, String input) {
        String email = User.normalizeEmail(rawEmail);
        VerificationCode code = store.findCode(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_EXPIRED));
        if (code.matches(input)) {
            store.deleteCode(email);
            store.markVerified(email);
            return;
        }
        if (code.isExhausted(store.recordFailure(email))) {
            store.deleteCode(email);
            throw new BusinessException(ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
        }
        throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
    }
}

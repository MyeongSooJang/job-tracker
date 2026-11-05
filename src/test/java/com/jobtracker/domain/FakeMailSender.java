package com.jobtracker.domain;

import com.jobtracker.domain.user.MailSender;
import com.jobtracker.domain.user.VerificationCode;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;

import java.util.HashMap;
import java.util.Map;

public class FakeMailSender implements MailSender {

    private final Map<String, String> lastCodes = new HashMap<>();
    private int sentCount;
    private boolean failing;

    public void failFromNow() {
        failing = true;
    }

    public void recoverFromNow() {
        failing = false;
    }

    public int sentCount() {
        return sentCount;
    }

    public String lastCodeSentTo(String email) {
        return lastCodes.get(email);
    }

    @Override
    public void sendVerificationCode(String email, VerificationCode code) {
        if (failing) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }
        sentCount++;
        lastCodes.put(email, code.value());
    }
}

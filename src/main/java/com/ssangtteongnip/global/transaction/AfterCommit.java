package com.ssangtteongnip.global.transaction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
public final class AfterCommit {

    private AfterCommit() {
    }

    // 트랜잭션이 있으면 커밋 뒤에, 없으면 바로 실행한다. 이미 끝난 요청을 깨뜨리지 않도록 실패는 로그만 남긴다.
    public static void runQuietly(String description, Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runLogging(description, action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runLogging(description, action);
            }
        });
    }

    private static void runLogging(String description, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("{} 실패", description, e);
        }
    }
}

package com.ssangtteongnip.global;

import com.ssangtteongnip.global.transaction.AfterCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AfterCommitTest {

    private final AtomicInteger runs = new AtomicInteger();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 트랜잭션이_없으면_바로_실행한다() {
        AfterCommit.runQuietly("테스트", runs::incrementAndGet);

        assertThat(runs.get()).isEqualTo(1);
    }

    @Test
    void 트랜잭션이_있으면_커밋_전에는_실행하지_않고_커밋_뒤에_실행한다() {
        TransactionSynchronizationManager.initSynchronization();

        AfterCommit.runQuietly("테스트", runs::incrementAndGet);
        assertThat(runs.get()).isZero();

        TransactionSynchronizationUtils.triggerAfterCommit();
        assertThat(runs.get()).isEqualTo(1);
    }

    @Test
    void 롤백되면_실행하지_않는다() {
        TransactionSynchronizationManager.initSynchronization();

        AfterCommit.runQuietly("테스트", runs::incrementAndGet);
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertThat(runs.get()).isZero();
    }

    @Test
    void 바로_실행하다_실패해도_예외를_던지지_않는다() {
        assertThatCode(() -> AfterCommit.runQuietly("테스트", () -> {
            throw new IllegalStateException("boom");
        })).doesNotThrowAnyException();
    }

    @Test
    void 커밋_뒤_실행이_실패해도_예외를_던지지_않는다() {
        TransactionSynchronizationManager.initSynchronization();
        AfterCommit.runQuietly("테스트", () -> {
            throw new IllegalStateException("boom");
        });

        assertThatCode(TransactionSynchronizationUtils::triggerAfterCommit).doesNotThrowAnyException();
    }
}

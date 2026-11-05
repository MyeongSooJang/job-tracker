package com.jobtracker.infrastructure;

import com.jobtracker.application.service.ResumeMigrationService;
import com.jobtracker.infrastructure.migration.LegacyResumeMigrationRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LegacyResumeMigrationRunnerTest {

    @Mock ResumeMigrationService migrationService;
    @InjectMocks LegacyResumeMigrationRunner runner;

    @Test
    void 한_건이_실패해도_나머지_이력서를_계속_옮긴다() {
        given(migrationService.findPendingIds()).willReturn(List.of(1L, 2L, 3L));
        willThrow(new IllegalStateException("boom")).given(migrationService).migrate(2L);

        runner.run(new DefaultApplicationArguments());

        verify(migrationService).migrate(1L);
        verify(migrationService).migrate(3L);
    }
}

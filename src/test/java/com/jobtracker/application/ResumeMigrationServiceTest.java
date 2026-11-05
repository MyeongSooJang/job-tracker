package com.jobtracker.application;

import com.jobtracker.application.service.ResumeMigrationService;
import com.jobtracker.domain.FakeFileStorage;
import com.jobtracker.domain.FakePasswordHasher;
import com.jobtracker.domain.profile.ResumeFile;
import com.jobtracker.domain.profile.ResumeFileRepository;
import com.jobtracker.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ResumeMigrationServiceTest {

    @Mock ResumeFileRepository resumeFileRepository;

    private final FakeFileStorage storage = new FakeFileStorage();
    private final User user = registeredUser();
    private ResumeMigrationService migrationService;

    private static User registeredUser() {
        User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private static byte[] pdf(String body) {
        return ("%PDF-1.7\n" + body).getBytes(StandardCharsets.US_ASCII);
    }

    @BeforeEach
    void setUp() {
        migrationService = new ResumeMigrationService(resumeFileRepository, storage);
    }

    @Test
    void 아직_옮기지_않은_이력서를_옮기면_기존_내용이_저장소에_올라가고_키가_채워진다() {
        byte[] legacy = pdf("legacy");
        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", legacy, new FakeFileStorage());
        ReflectionTestUtils.setField(resume, "objectKey", null);
        ReflectionTestUtils.setField(resume, "legacyContent", legacy);
        given(resumeFileRepository.findById(10L)).willReturn(Optional.of(resume));

        migrationService.migrate(10L);

        assertThat(resume.getObjectKey()).isNotNull();
        assertThat(storage.contentOf(resume.getObjectKey())).isEqualTo(legacy);
    }

    @Test
    void 이미_옮긴_이력서를_다시_옮기면_저장소가_바뀌지_않는다() {
        ResumeFile resume = ResumeFile.upload(user, "이력서.pdf", pdf("resume"), storage);
        String key = resume.getObjectKey();
        given(resumeFileRepository.findById(10L)).willReturn(Optional.of(resume));

        migrationService.migrate(10L);

        assertThat(resume.getObjectKey()).isEqualTo(key);
        assertThat(storage.size()).isEqualTo(1);
    }
}

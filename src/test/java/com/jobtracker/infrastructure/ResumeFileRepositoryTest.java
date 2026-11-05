package com.jobtracker.infrastructure;

import com.jobtracker.domain.FakeFileStorage;
import com.jobtracker.domain.FakePasswordHasher;
import com.jobtracker.domain.profile.PersonalInfo;
import com.jobtracker.domain.profile.Profile;
import com.jobtracker.domain.profile.ResumeFile;
import com.jobtracker.domain.profile.ResumeSummary;
import com.jobtracker.domain.user.User;
import com.jobtracker.global.config.JpaConfig;
import com.jobtracker.domain.profile.ProfileRepository;
import com.jobtracker.domain.profile.ResumeFileRepository;
import com.jobtracker.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class ResumeFileRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired ProfileRepository profileRepository;
    @Autowired ResumeFileRepository resumeFileRepository;
    @Autowired EntityManager em;

    private final FakeFileStorage storage = new FakeFileStorage();

    private User saveUser() {
        return userRepository.save(User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher()));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void 이력서의_파일명_키_콘텐츠_타입을_저장하고_읽어온다() {
        User user = saveUser();
        ResumeFile saved = resumeFileRepository.save(ResumeFile.upload(user, "이력서.pdf",
                "%PDF-1.7 body".getBytes(StandardCharsets.US_ASCII), storage));
        String objectKey = saved.getObjectKey();
        flushAndClear();

        ResumeFile found = resumeFileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(found.getObjectKey()).isEqualTo(objectKey);
        assertThat(found.getContentType()).isEqualTo("application/pdf");
        assertThat(found.getFileName()).isEqualTo("이력서.pdf");
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void 요약_조회는_파일명_크기_수정시각을_반환한다() {
        User user = saveUser();
        byte[] content = "%PDF-1.7 body".getBytes(StandardCharsets.US_ASCII);
        resumeFileRepository.save(ResumeFile.upload(user, "이력서.pdf", content, storage));
        flushAndClear();

        ResumeSummary summary = resumeFileRepository.findSummaryByUserId(user.getId()).orElseThrow();

        assertThat(summary.fileName()).isEqualTo("이력서.pdf");
        assertThat(summary.sizeBytes()).isEqualTo(content.length);
        assertThat(summary.updatedAt()).isNotNull();
    }

    @Test
    void 이력서가_없으면_빈_결과를_반환한다() {
        User user = saveUser();

        assertThat(resumeFileRepository.findByUserId(user.getId())).isEmpty();
        assertThat(resumeFileRepository.findSummaryByUserId(user.getId())).isEmpty();
    }

    @Test
    void 이력서를_삭제한다() {
        User user = saveUser();
        ResumeFile saved = resumeFileRepository.save(ResumeFile.upload(user, "a.pdf",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII), storage));
        flushAndClear();

        resumeFileRepository.delete(resumeFileRepository.findByUserId(user.getId()).orElseThrow());
        flushAndClear();

        assertThat(em.find(ResumeFile.class, saved.getId())).isNull();
    }

    @Test
    void 프로필의_인적_사항을_저장하고_조회한다() {
        User user = saveUser();
        Profile profile = profileRepository.save(Profile.create(user));
        profile.update("홍길순", PersonalInfo.of("010-1234-5678", LocalDate.of(1998, 4, 27), "서울시",
                "https://github.com/hong", null, null, "OO대", "인턴", "백엔드", "안녕하세요", null));
        flushAndClear();

        Profile found = profileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(found.getUser().getName()).isEqualTo("홍길순");
        assertThat(found.getInfo().getBirthDate()).isEqualTo(LocalDate.of(1998, 4, 27));
        assertThat(found.getInfo().getDesiredJob()).isEqualTo("백엔드");
    }

    @Test
    void 인적_사항이_모두_비어_있는_프로필도_다시_조회하면_빈_값으로_읽힌다() {
        User user = saveUser();
        profileRepository.save(Profile.create(user));
        flushAndClear();

        Profile found = profileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(found.getInfo()).isNotNull();
        assertThat(found.getInfo().getPhone()).isNull();
        assertThat(found.getInfo().getBirthDate()).isNull();
    }
}

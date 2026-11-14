package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.FakeFileStorage;
import com.ssangtteongnip.domain.FakePasswordHasher;
import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.domain.profile.UserFile;
import com.ssangtteongnip.domain.profile.UserFileRepository;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class UserFileRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired UserFileRepository userFileRepository;
    @Autowired EntityManager em;

    private final FakeFileStorage storage = new FakeFileStorage();

    private User saveUser(String email) {
        return userRepository.save(User.register(email, "홍길동", "password1", new FakePasswordHasher()));
    }

    private UserFile saveFile(User user, FileType type, String fileName) {
        return userFileRepository.save(UserFile.upload(user, type, fileName,
                "%PDF-1.7 body".getBytes(StandardCharsets.US_ASCII), storage));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void 파일의_종류_이름_키_콘텐츠_타입_업로드시각을_저장하고_읽어온다() {
        User user = saveUser("hong@example.com");
        UserFile saved = saveFile(user, FileType.CERTIFICATE, "자격증.pdf");
        String objectKey = saved.getObjectKey();
        flushAndClear();

        UserFile found = userFileRepository.findByIdAndUserId(saved.getId(), user.getId()).orElseThrow();

        assertThat(found.getType()).isEqualTo(FileType.CERTIFICATE);
        assertThat(found.getFileName()).isEqualTo("자격증.pdf");
        assertThat(found.getObjectKey()).isEqualTo(objectKey);
        assertThat(found.getContentType()).isEqualTo("application/pdf");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void 같은_종류의_파일도_여러_개_저장할_수_있다() {
        User user = saveUser("hong@example.com");
        saveFile(user, FileType.RESUME, "이력서v1.pdf");
        saveFile(user, FileType.RESUME, "이력서v2.pdf");
        flushAndClear();

        assertThat(userFileRepository.countByUserId(user.getId())).isEqualTo(2);
    }

    @Test
    void 목록은_나중에_올린_파일이_먼저_나온다() {
        User user = saveUser("hong@example.com");
        saveFile(user, FileType.RESUME, "첫번째.pdf");
        saveFile(user, FileType.PORTFOLIO, "두번째.pdf");
        saveFile(user, FileType.ETC, "세번째.pdf");
        flushAndClear();

        List<UserFile> files = userFileRepository.findUserFilesNewestFirst(user.getId());

        assertThat(files).extracting(UserFile::getFileName)
                .containsExactly("세번째.pdf", "두번째.pdf", "첫번째.pdf");
    }

    @Test
    void 목록과_개수에는_다른_사용자의_파일이_포함되지_않는다() {
        User owner = saveUser("hong@example.com");
        User other = saveUser("kim@example.com");
        saveFile(owner, FileType.RESUME, "내것.pdf");
        saveFile(other, FileType.RESUME, "남의것.pdf");
        flushAndClear();

        assertThat(userFileRepository.findUserFilesNewestFirst(owner.getId()))
                .extracting(UserFile::getFileName).containsExactly("내것.pdf");
        assertThat(userFileRepository.countByUserId(owner.getId())).isEqualTo(1);
    }

    @Test
    void 다른_사용자의_파일은_아이디로도_찾을_수_없다() {
        User owner = saveUser("hong@example.com");
        User other = saveUser("kim@example.com");
        UserFile saved = saveFile(owner, FileType.RESUME, "내것.pdf");
        flushAndClear();

        assertThat(userFileRepository.findByIdAndUserId(saved.getId(), other.getId())).isEmpty();
    }

    @Test
    void 파일이_없으면_개수는_0이고_목록은_비어_있다() {
        User user = saveUser("hong@example.com");

        assertThat(userFileRepository.countByUserId(user.getId())).isZero();
        assertThat(userFileRepository.findUserFilesNewestFirst(user.getId())).isEmpty();
    }

    @Test
    void 파일을_삭제한다() {
        User user = saveUser("hong@example.com");
        UserFile saved = saveFile(user, FileType.RESUME, "a.pdf");
        flushAndClear();

        userFileRepository.delete(userFileRepository.findByIdAndUserId(saved.getId(), user.getId()).orElseThrow());
        flushAndClear();

        assertThat(em.find(UserFile.class, saved.getId())).isNull();
    }

    @Test
    void 사용자_행을_잠그고_조회한다() {
        User user = saveUser("hong@example.com");
        flushAndClear();

        assertThat(userRepository.findByIdForUpdate(user.getId())).isPresent();
        assertThat(userRepository.findByIdForUpdate(user.getId() + 999)).isEmpty();
    }
}

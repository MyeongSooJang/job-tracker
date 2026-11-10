package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.FakeFileStorage;
import com.ssangtteongnip.domain.image.StudyImage;
import com.ssangtteongnip.domain.image.StudyImageRepository;
import com.ssangtteongnip.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class StudyImageRepositoryTest {

    private static final long USER_ID = 3L;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Autowired StudyImageRepository studyImageRepository;
    @Autowired EntityManager em;

    private final FakeFileStorage storage = new FakeFileStorage();

    private StudyImage saveAndReload() {
        StudyImage saved = studyImageRepository.save(StudyImage.upload(USER_ID, PNG, storage));
        em.flush();
        em.clear();
        return studyImageRepository.findByPublicId(saved.getPublicId()).orElseThrow();
    }

    @Test
    void 저장한_이미지는_공개_아이디로_다시_찾을_수_있고_모든_값이_보존된다() {
        StudyImage found = saveAndReload();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getUserId()).isEqualTo(USER_ID);
        assertThat(found.getContentType()).isEqualTo("image/png");
        assertThat(found.getSizeBytes()).isEqualTo(PNG.length);
        assertThat(found.getObjectKey()).startsWith("study-images/3/").endsWith(".png");
        assertThat(storage.contains(found.getObjectKey())).isTrue();
    }

    @Test
    void 저장하면_생성_시각이_기록된다() {
        assertThat(saveAndReload().getCreatedAt()).isNotNull();
    }

    @Test
    void 다시_찾은_이미지의_본문_경로와_저장소_주소는_저장_전과_같다() {
        StudyImage original = studyImageRepository.save(StudyImage.upload(USER_ID, PNG, storage));
        String path = original.publicPath();
        em.flush();
        em.clear();

        StudyImage found = studyImageRepository.findByPublicId(original.getPublicId()).orElseThrow();

        assertThat(found.publicPath()).isEqualTo(path);
        assertThat(found.viewUrl(storage).url()).isEqualTo("https://fake-storage/" + found.getObjectKey());
    }

    @Test
    void 지운_이미지는_더는_찾을_수_없고_다른_이미지는_남는다() {
        StudyImage deleted = studyImageRepository.save(StudyImage.upload(USER_ID, PNG, storage));
        StudyImage kept = studyImageRepository.save(StudyImage.upload(USER_ID, PNG, storage));
        em.flush();

        studyImageRepository.delete(deleted);
        em.flush();
        em.clear();

        assertThat(studyImageRepository.findByPublicId(deleted.getPublicId())).isEmpty();
        assertThat(studyImageRepository.findByPublicId(kept.getPublicId())).isPresent();
    }

    @Test
    void 없는_공개_아이디는_찾을_수_없다() {
        Optional<StudyImage> found = studyImageRepository.findByPublicId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void 같은_사용자가_여러_장을_올려도_공개_아이디로_각각_구분된다() {
        StudyImage first = studyImageRepository.save(StudyImage.upload(USER_ID, PNG, storage));
        StudyImage second = studyImageRepository.save(StudyImage.upload(USER_ID, PNG, storage));
        em.flush();
        em.clear();

        assertThat(studyImageRepository.findByPublicId(first.getPublicId()).orElseThrow().getId())
                .isEqualTo(first.getId());
        assertThat(studyImageRepository.findByPublicId(second.getPublicId()).orElseThrow().getId())
                .isEqualTo(second.getId());
    }
}

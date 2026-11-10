package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.studylog.StudyLog;
import com.ssangtteongnip.domain.studylog.StudyLogRepository;
import com.ssangtteongnip.global.config.JpaConfig;
import com.ssangtteongnip.infrastructure.persistence.StudyLogJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class StudyLogRepositoryTest {

    private static final Long OWNER = 1L;
    private static final Long OTHER = 2L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @Autowired StudyLogJpaRepository studyLogRepository;

    private StudyLog save(Long userId, LocalDate date, String title) {
        // 미래 날짜 검증은 도메인 테스트에서 다루므로, 어떤 날짜든 저장할 수 있게 기록일을 오늘로 넘긴다.
        return studyLogRepository.saveAndFlush(StudyLog.create(userId, date, title, "공부 내용", 60, date));
    }

    private StudyLog save(Long userId, LocalDate date) {
        return save(userId, date, "제목");
    }

    @Test
    void 같은_사용자가_같은_날짜에_여러_글을_저장할_수_있다() {
        save(OWNER, TODAY, "아침");
        save(OWNER, TODAY, "저녁");

        List<StudyLog> logs = studyLogRepository.findInPeriod(
                OWNER, TODAY, TODAY);

        assertThat(logs).extracting(StudyLog::getTitle).containsExactly("저녁", "아침");
    }

    @Test
    void 글_id와_사용자로_조회한다() {
        StudyLog saved = save(OWNER, TODAY);

        assertThat(studyLogRepository.findByIdAndUserId(saved.getId(), OWNER)).isPresent();
    }

    @Test
    void 다른_사용자의_글은_id를_알아도_조회되지_않는다() {
        StudyLog saved = save(OTHER, TODAY);

        assertThat(studyLogRepository.findByIdAndUserId(saved.getId(), OWNER)).isEmpty();
        assertThat(studyLogRepository.findStudyDatesByUserId(OWNER)).isEmpty();
        assertThat(studyLogRepository.findInPeriod(
                OWNER, TODAY.minusDays(30), TODAY)).isEmpty();
    }

    @Test
    void 구간_조회는_양쪽_끝_날짜를_포함하고_최신_날짜부터_돌려준다() {
        save(OWNER, LocalDate.of(2026, 10, 31));
        save(OWNER, LocalDate.of(2026, 11, 30));
        save(OWNER, LocalDate.of(2026, 11, 1));
        save(OWNER, LocalDate.of(2026, 12, 1));

        List<StudyLog> logs = studyLogRepository.findInPeriod(
                OWNER, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(logs).extracting(StudyLog::getStudyDate)
                .containsExactly(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1));
    }

    @Test
    void 기록한_날짜는_하루에_여러_글이_있어도_한_번만_최신순으로_돌려준다() {
        save(OWNER, LocalDate.of(2026, 11, 1));
        save(OWNER, LocalDate.of(2026, 11, 3));
        save(OWNER, LocalDate.of(2026, 11, 3));
        save(OWNER, LocalDate.of(2026, 11, 2));

        assertThat(studyLogRepository.findStudyDatesByUserId(OWNER)).containsExactly(
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1));
    }

    @Test
    void 기간_안에서_글이_있는_날짜_수를_중복_없이_센다() {
        save(OWNER, LocalDate.of(2026, 10, 31));
        save(OWNER, LocalDate.of(2026, 11, 1));
        save(OWNER, LocalDate.of(2026, 11, 1));
        save(OWNER, LocalDate.of(2026, 11, 3));
        save(OTHER, LocalDate.of(2026, 11, 2));

        long count = studyLogRepository.countStudyDatesBetween(
                OWNER, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(count).isEqualTo(2);
    }

    private StudyLog saveWithContent(Long userId, String content) {
        return studyLogRepository.saveAndFlush(StudyLog.create(userId, TODAY, "제목", content, 60, TODAY));
    }

    @Test
    void 본문에_문자열이_들어_있는_글이_있으면_참조하고_있다고_답한다() {
        saveWithContent(OWNER, "앞 ![](/api/images/3f2b8c1e-0000-4000-8000-000000000001) 뒤");

        assertThat(studyLogRepository.existsByContentContaining("/api/images/3f2b8c1e-0000-4000-8000-000000000001")).isTrue();
    }

    @Test
    void 어느_글의_본문에도_없으면_참조하지_않는다고_답한다() {
        saveWithContent(OWNER, "이미지 없는 글");

        assertThat(studyLogRepository.existsByContentContaining("/api/images/3f2b8c1e-0000-4000-8000-000000000001")).isFalse();
    }

    @Test
    void 다른_사용자의_글이_참조해도_참조하고_있다고_답한다() {
        saveWithContent(OTHER, "![](/api/images/3f2b8c1e-0000-4000-8000-000000000001)");

        assertThat(studyLogRepository.existsByContentContaining("/api/images/3f2b8c1e-0000-4000-8000-000000000001")).isTrue();
    }

    @Test
    void 밑줄과_퍼센트는_와일드카드가_아니라_글자_그대로_찾는다() {
        saveWithContent(OWNER, "abc");

        assertThat(studyLogRepository.existsByContentContaining("a_c")).isFalse();
        assertThat(studyLogRepository.existsByContentContaining("a%c")).isFalse();
    }

    @Test
    void 글_본문을_바꾸면_따로_저장하지_않아도_바뀐_본문_기준으로_찾는다() {
        StudyLog saved = saveWithContent(OWNER, "![](/api/images/3f2b8c1e-0000-4000-8000-000000000001)");

        saved.write("제목", "이미지를 뺀 새 본문", 60);

        // 서비스는 글을 고친 같은 트랜잭션에서 곧바로 이 질문을 한다. 변경이 반영되어 있어야 이미지를 지울 수 있다.
        assertThat(studyLogRepository.existsByContentContaining("/api/images/3f2b8c1e-0000-4000-8000-000000000001")).isFalse();
    }

    @Test
    void 글을_지우면_따로_저장하지_않아도_더는_참조하지_않는다() {
        StudyLog saved = saveWithContent(OWNER, "![](/api/images/3f2b8c1e-0000-4000-8000-000000000001)");

        // 서비스가 쓰는 도메인 저장소 인터페이스로 지운다.
        ((StudyLogRepository) studyLogRepository).delete(saved);

        assertThat(studyLogRepository.existsByContentContaining("/api/images/3f2b8c1e-0000-4000-8000-000000000001")).isFalse();
    }
}

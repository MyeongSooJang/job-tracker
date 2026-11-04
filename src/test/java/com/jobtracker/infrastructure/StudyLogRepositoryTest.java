package com.jobtracker.infrastructure;

import com.jobtracker.domain.studylog.StudyLog;
import com.jobtracker.global.config.JpaConfig;
import com.jobtracker.infrastructure.persistence.StudyLogJpaRepository;
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

        List<StudyLog> logs = studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(
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
        assertThat(studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(
                OWNER, TODAY.minusDays(30), TODAY)).isEmpty();
    }

    @Test
    void 구간_조회는_양쪽_끝_날짜를_포함하고_최신_날짜부터_돌려준다() {
        save(OWNER, LocalDate.of(2026, 10, 31));
        save(OWNER, LocalDate.of(2026, 11, 30));
        save(OWNER, LocalDate.of(2026, 11, 1));
        save(OWNER, LocalDate.of(2026, 12, 1));

        List<StudyLog> logs = studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateDescIdDesc(
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
}

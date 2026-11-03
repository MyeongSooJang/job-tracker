package com.jobtracker.infrastructure;

import com.jobtracker.domain.studylog.StudyLog;
import com.jobtracker.global.config.JpaConfig;
import com.jobtracker.infrastructure.persistence.StudyLogJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaConfig.class)
class StudyLogRepositoryTest {

    private static final Long OWNER = 1L;
    private static final Long OTHER = 2L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @Autowired StudyLogJpaRepository studyLogRepository;

    private StudyLog save(Long userId, LocalDate date) {
        // 미래 날짜 검증은 도메인 테스트에서 다루므로, 어떤 날짜든 저장할 수 있게 기록일을 오늘로 넘긴다.
        return studyLogRepository.saveAndFlush(StudyLog.create(userId, date, "공부 내용", 60, date));
    }

    @Test
    void 같은_사용자의_같은_날짜에는_기록을_두_개_저장할_수_없다() {
        save(OWNER, TODAY);

        assertThatThrownBy(() -> save(OWNER, TODAY)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 다른_사용자는_같은_날짜에_각자_기록할_수_있다() {
        save(OWNER, TODAY);
        save(OTHER, TODAY);

        assertThat(studyLogRepository.findByUserIdAndStudyDate(OWNER, TODAY)).isPresent();
        assertThat(studyLogRepository.findByUserIdAndStudyDate(OTHER, TODAY)).isPresent();
    }

    @Test
    void 다른_사용자의_기록은_조회되지_않는다() {
        save(OTHER, TODAY);

        assertThat(studyLogRepository.findByUserIdAndStudyDate(OWNER, TODAY)).isEmpty();
        assertThat(studyLogRepository.findStudyDatesByUserId(OWNER)).isEmpty();
        assertThat(studyLogRepository.findTop10ByUserIdOrderByStudyDateDesc(OWNER)).isEmpty();
    }

    @Test
    void 기간_조회는_양쪽_끝_날짜를_포함하고_날짜_오름차순이다() {
        save(OWNER, LocalDate.of(2026, 10, 31));
        save(OWNER, LocalDate.of(2026, 11, 30));
        save(OWNER, LocalDate.of(2026, 11, 1));
        save(OWNER, LocalDate.of(2026, 12, 1));

        List<StudyLog> logs = studyLogRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(
                OWNER, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(logs).extracting(StudyLog::getStudyDate)
                .containsExactly(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    }

    @Test
    void 기간_안의_기록_개수를_센다() {
        save(OWNER, LocalDate.of(2026, 10, 31));
        save(OWNER, LocalDate.of(2026, 11, 1));
        save(OWNER, LocalDate.of(2026, 11, 3));
        save(OTHER, LocalDate.of(2026, 11, 2));

        long count = studyLogRepository.countByUserIdAndStudyDateBetween(
                OWNER, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void 최근_기록은_최신_날짜부터_10건만_돌려준다() {
        IntStream.range(0, 12).forEach(daysAgo -> save(OWNER, TODAY.minusDays(daysAgo)));

        List<StudyLog> logs = studyLogRepository.findTop10ByUserIdOrderByStudyDateDesc(OWNER);

        assertThat(logs).hasSize(10);
        assertThat(logs.get(0).getStudyDate()).isEqualTo(TODAY);
        assertThat(logs.get(9).getStudyDate()).isEqualTo(TODAY.minusDays(9));
    }

    @Test
    void 기록한_날짜는_최신순으로_돌려준다() {
        save(OWNER, LocalDate.of(2026, 11, 1));
        save(OWNER, LocalDate.of(2026, 11, 3));
        save(OWNER, LocalDate.of(2026, 11, 2));

        assertThat(studyLogRepository.findStudyDatesByUserId(OWNER)).containsExactly(
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1));
    }
}

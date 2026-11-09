package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.application.ApplicationRepository;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.company.CompanyRepository;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.domain.review.ReviewRepository;
import com.ssangtteongnip.domain.schedule.Schedule;
import com.ssangtteongnip.domain.schedule.ScheduleRepository;
import com.ssangtteongnip.domain.schedule.ScheduleType;
import com.ssangtteongnip.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class OwnershipRepositoryTest {

    private static final Long OWNER = 1L;
    private static final Long OTHER = 2L;

    @Autowired CompanyRepository companyRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired EntityManager em;

    private final LocalDateTime scheduledAt = LocalDateTime.of(2026, 11, 10, 10, 0);

    @BeforeEach
    void setUp() {
        Company company = companyRepository.save(Company.create("카카오", "IT", "https://kakao.com"));
        save(company, OWNER);
        save(company, OTHER);
        em.flush();
        em.clear();
    }

    private void save(Company company, Long userId) {
        Application application = applicationRepository.save(
                Application.create(userId, company, "백엔드 개발자", LocalDate.now()));
        scheduleRepository.save(Schedule.create(application, "1차 면접", scheduledAt,
                ScheduleType.INTERVIEW, "판교", ""));
        reviewRepository.save(Review.create(application));
    }

    @Test
    void 지원_건은_소유자의_것만_조회한다() {
        assertThat(applicationRepository.findByUserId(OWNER)).hasSize(1)
                .allMatch(application -> application.isOwnedBy(OWNER));
        assertThat(applicationRepository.findByUserIdAndStatus(OWNER, ApplicationStatus.RESUME_SUBMITTED)).hasSize(1);
        assertThat(applicationRepository.findByUserIdAndStatus(OWNER, ApplicationStatus.INTERVIEW)).isEmpty();
    }

    @Test
    void 일정은_소유한_지원_건의_것만_조회한다() {
        assertThat(scheduleRepository.findByApplicationUserId(OWNER)).hasSize(1);
        assertThat(scheduleRepository.findInPeriod(
                OWNER, scheduledAt.minusDays(1), scheduledAt.plusDays(1))).hasSize(1);
        assertThat(scheduleRepository.findInPeriod(
                OWNER, scheduledAt.plusDays(1), scheduledAt.plusDays(2))).isEmpty();
    }

    @Test
    void 후기는_소유한_지원_건의_것만_조회한다() {
        assertThat(reviewRepository.findByApplicationUserId(OWNER)).hasSize(1);
        assertThat(reviewRepository.findByApplicationUserId(3L)).isEmpty();
    }
}

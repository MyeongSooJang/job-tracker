package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.application.ApplicationRepository;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.application.JobSite;
import com.ssangtteongnip.domain.application.JobSiteStatistics;
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
    // setUp이 사이트 없는 지원 건을 저장하므로 집계 테스트는 별도 사용자로 격리한다.
    private static final Long STATS_OWNER = 10L;
    private static final Long STATS_OTHER = 11L;
    private static final Long NO_APPLICATION_USER = 12L;

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

    private void saveApplication(Long userId, JobSite jobSite, boolean documentPassed) {
        Company company = companyRepository.findAll().get(0);
        Application application = Application.create(userId, company, "백엔드 개발자", LocalDate.now(), jobSite);
        if (documentPassed) {
            application.startInterview(scheduledAt);
        }
        applicationRepository.save(application);
    }

    @Test
    void 사이트별_집계는_소유자의_지원_건만_사이트별_분모와_분자로_센다() {
        saveApplication(STATS_OWNER, JobSite.SARAMIN, true);
        saveApplication(STATS_OWNER, JobSite.SARAMIN, true);
        saveApplication(STATS_OWNER, JobSite.SARAMIN, false);
        saveApplication(STATS_OWNER, JobSite.WANTED, false);
        saveApplication(STATS_OTHER, JobSite.SARAMIN, true);
        saveApplication(STATS_OTHER, JobSite.WANTED, true);
        em.flush();
        em.clear();

        assertThat(applicationRepository.summarizeByJobSite(STATS_OWNER)).containsExactly(
                new JobSiteStatistics(JobSite.SARAMIN, 3, 2),
                new JobSiteStatistics(JobSite.WANTED, 1, 0));
    }

    @Test
    void 사이트별_집계는_서류_탈락_후_불합격과_면접_후_불합격을_구분한다() {
        saveApplication(STATS_OWNER, JobSite.JOBKOREA, false);
        Company company = companyRepository.findAll().get(0);
        Application rejectedAfterDocument = Application.create(STATS_OWNER, company, "백엔드 개발자", LocalDate.now(), JobSite.JOBKOREA);
        rejectedAfterDocument.advanceStatus(ApplicationStatus.REJECTED);
        Application rejectedAfterInterview = Application.create(STATS_OWNER, company, "백엔드 개발자", LocalDate.now(), JobSite.JOBKOREA);
        rejectedAfterInterview.startInterview(scheduledAt);
        rejectedAfterInterview.advanceStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(rejectedAfterDocument);
        applicationRepository.save(rejectedAfterInterview);
        em.flush();
        em.clear();

        assertThat(applicationRepository.summarizeByJobSite(STATS_OWNER))
                .containsExactly(new JobSiteStatistics(JobSite.JOBKOREA, 3, 1));
    }

    @Test
    void 사이트별_집계는_지원_건_수가_같으면_사이트_이름순으로_정렬한다() {
        saveApplication(STATS_OWNER, JobSite.WANTED, false);
        saveApplication(STATS_OWNER, JobSite.JOBKOREA, false);
        saveApplication(STATS_OWNER, JobSite.SARAMIN, false);
        saveApplication(STATS_OWNER, JobSite.SARAMIN, false);
        em.flush();
        em.clear();

        assertThat(applicationRepository.summarizeByJobSite(STATS_OWNER))
                .extracting(JobSiteStatistics::jobSite)
                .containsExactly(JobSite.SARAMIN, JobSite.JOBKOREA, JobSite.WANTED);
    }

    @Test
    void 지원이_없는_사용자의_사이트별_집계는_비어_있다() {
        assertThat(applicationRepository.summarizeByJobSite(NO_APPLICATION_USER)).isEmpty();
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

package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.application.ApplicationRepository;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.application.JobSiteStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationJpaRepository extends JpaRepository<Application, Long>, ApplicationRepository {
    List<Application> findByUserId(Long userId);

    List<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status);

    @Override
    @Query("""
            select new com.ssangtteongnip.domain.application.JobSiteStatistics(
                a.jobSite, count(a), count(a.documentPassedAt))
            from Application a
            where a.userId = :userId
            group by a.jobSite
            order by count(a) desc, a.jobSite asc
            """)
    List<JobSiteStatistics> summarizeByJobSite(@Param("userId") Long userId);

    @Override
    @Query("""
            select a from Application a
            where a.status = com.ssangtteongnip.domain.application.ApplicationStatus.RESUME_SUBMITTED
              and a.deadline is not null
            """)
    List<Application> findPendingWithDeadline();
}

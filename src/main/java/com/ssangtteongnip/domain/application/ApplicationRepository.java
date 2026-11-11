package com.ssangtteongnip.domain.application;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository {
    Application save(Application application);
    Optional<Application> findById(Long id);
    List<Application> findByUserId(Long userId);
    List<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status);

    // 사용자의 지원 건을 구직 사이트별로 집계한다. 지원이 없는 사이트는 포함하지 않고,
    // 지원 건 수 내림차순, 같으면 사이트 이름 오름차순으로 정렬한다.
    List<JobSiteStatistics> summarizeByJobSite(Long userId);
}

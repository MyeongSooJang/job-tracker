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

    // 서류 제출 상태이면서 공고 마감일이 있는 모든 사용자의 지원 건을 조회한다. 정렬은 보장하지 않는다.
    // 마감일 + 1달이 지났는지는 조회하지 않고 도메인(Application)이 판단한다.
    List<Application> findPendingWithDeadline();
}

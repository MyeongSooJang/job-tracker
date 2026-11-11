package com.ssangtteongnip.domain.application;

// 구직 사이트 하나의 지원 건 집계. 서류 통과율은 (서류 통과 건 수) / (전체 지원 건 수)이다.
public record JobSiteStatistics(JobSite jobSite, long applicationCount, long documentPassedCount) {

    // 0.0 ~ 1.0. 지원이 없으면 0이다.
    public double documentPassRate() {
        if (applicationCount == 0) {
            return 0;
        }
        return (double) documentPassedCount / applicationCount;
    }
}

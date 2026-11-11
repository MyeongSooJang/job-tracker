package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.application.JobSite;
import com.ssangtteongnip.domain.application.JobSiteStatistics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JobSiteStatisticsTest {

    @Test
    void 서류_통과율은_통과_건_수를_전체_지원_건_수로_나눈_값이다() {
        JobSiteStatistics statistics = new JobSiteStatistics(JobSite.SARAMIN, 10, 3);

        assertThat(statistics.documentPassRate()).isCloseTo(0.3, within(1e-9));
    }

    @Test
    void 지원이_모두_서류를_통과하면_통과율은_1이다() {
        assertThat(new JobSiteStatistics(JobSite.WANTED, 4, 4).documentPassRate()).isEqualTo(1.0);
    }

    @Test
    void 통과한_지원이_없으면_통과율은_0이다() {
        assertThat(new JobSiteStatistics(JobSite.WANTED, 4, 0).documentPassRate()).isZero();
    }

    @Test
    void 지원이_0건이면_0으로_나누지_않고_통과율은_0이다() {
        assertThat(new JobSiteStatistics(JobSite.OTHER, 0, 0).documentPassRate()).isZero();
    }
}

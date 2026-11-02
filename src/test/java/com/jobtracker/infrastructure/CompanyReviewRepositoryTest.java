package com.jobtracker.infrastructure;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationRepository;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.company.CompanyRepository;
import com.jobtracker.domain.review.CompanyReviewSummary;
import com.jobtracker.domain.review.Review;
import com.jobtracker.domain.review.ReviewRepository;
import com.jobtracker.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@Import(JpaConfig.class)
class CompanyReviewRepositoryTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired EntityManager em;

    private Company kakao;
    private Company naver;
    private Company toss;

    @BeforeEach
    void setUp() {
        kakao = companyRepository.save(Company.create("카카오", "IT", "https://kakao.com"));
        naver = companyRepository.save(Company.create("네이버", "IT", "https://naver.com"));
        toss = companyRepository.save(Company.create("토스", "핀테크", "https://toss.im"));
    }

    private void writtenReview(Company company, long userId, String content, int rating) {
        Review review = unwrittenReview(company, userId);
        review.write(content, rating);
    }

    private Review unwrittenReview(Company company, long userId) {
        Application application = applicationRepository.save(
                Application.create(userId, company, "백엔드 개발자", LocalDate.of(2026, 10, 1)));
        return reviewRepository.save(Review.create(application));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void 회사의_후기는_작성된_것만_최신순으로_조회한다() {
        writtenReview(kakao, 1L, "먼저 쓴 후기", 3);
        writtenReview(kakao, 2L, "나중에 쓴 후기", 5);
        unwrittenReview(kakao, 3L);
        flushAndClear();

        assertThat(reviewRepository.findWrittenByCompanyId(kakao.getId()))
                .extracting(Review::getContent)
                .containsExactly("나중에 쓴 후기", "먼저 쓴 후기");
    }

    @Test
    void 다른_회사의_후기는_조회하지_않는다() {
        writtenReview(kakao, 1L, "카카오 후기", 4);
        writtenReview(naver, 2L, "네이버 후기", 2);
        flushAndClear();

        assertThat(reviewRepository.findWrittenByCompanyId(naver.getId()))
                .extracting(Review::getContent)
                .containsExactly("네이버 후기");
    }

    @Test
    void 회사의_후기_요약은_작성된_후기의_개수와_평균_평점이다() {
        writtenReview(kakao, 1L, "후기1", 5);
        writtenReview(kakao, 2L, "후기2", 4);
        unwrittenReview(kakao, 3L);
        flushAndClear();

        CompanyReviewSummary summary = reviewRepository.summarizeByCompanyId(kakao.getId()).orElseThrow();

        assertThat(summary.companyId()).isEqualTo(kakao.getId());
        assertThat(summary.reviewCount()).isEqualTo(2);
        assertThat(summary.averageRating()).isEqualTo(4.5);
    }

    @Test
    void 작성된_후기가_없는_회사는_요약이_없다() {
        unwrittenReview(kakao, 1L);
        flushAndClear();

        assertThat(reviewRepository.summarizeByCompanyId(kakao.getId())).isEmpty();
        assertThat(reviewRepository.summarizeByCompanyId(toss.getId())).isEmpty();
    }

    @Test
    void 회사별_후기_요약을_한번에_조회한다() {
        writtenReview(kakao, 1L, "후기1", 5);
        writtenReview(kakao, 2L, "후기2", 3);
        writtenReview(naver, 3L, "후기3", 2);
        flushAndClear();

        assertThat(reviewRepository.summarizeByCompany())
                .extracting(CompanyReviewSummary::companyId, CompanyReviewSummary::reviewCount,
                        CompanyReviewSummary::averageRating)
                .containsExactlyInAnyOrder(
                        tuple(kakao.getId(), 2L, 4.0),
                        tuple(naver.getId(), 1L, 2.0));
    }
}

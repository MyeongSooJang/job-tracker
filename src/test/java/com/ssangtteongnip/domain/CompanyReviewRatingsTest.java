package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.review.CompanyReviewRating;
import com.ssangtteongnip.domain.review.CompanyReviewRatings;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyReviewRatingsTest {

    private Company companyWithId(Long id) {
        Company company = Company.create("회사" + id, "IT", "https://example.com/" + id);
        ReflectionTestUtils.setField(company, "id", id);
        return company;
    }

    @Test
    void 평점이_집계된_회사는_집계된_후기_수와_평균_평점을_준다() {
        CompanyReviewRatings ratings = new CompanyReviewRatings(List.of(
                new CompanyReviewRating(1L, 2, 4.5),
                new CompanyReviewRating(2L, 1, 3.0)));

        CompanyReviewRating rating = ratings.findCompanyRating(companyWithId(2L));

        assertThat(rating.reviewCount()).isEqualTo(1);
        assertThat(rating.averageRating()).isEqualTo(3.0);
    }

    @Test
    void 평점이_집계되지_않은_회사는_후기_0개와_평균_없음을_준다() {
        CompanyReviewRatings ratings = new CompanyReviewRatings(List.of(
                new CompanyReviewRating(1L, 2, 4.5)));

        CompanyReviewRating rating = ratings.findCompanyRating(companyWithId(2L));

        assertThat(rating.companyId()).isEqualTo(2L);
        assertThat(rating.reviewCount()).isZero();
        assertThat(rating.averageRating()).isNull();
    }

    @Test
    void 집계된_평점이_하나도_없어도_회사의_평점을_줄_수_있다() {
        CompanyReviewRatings ratings = new CompanyReviewRatings(List.of());

        CompanyReviewRating rating = ratings.findCompanyRating(companyWithId(1L));

        assertThat(rating.reviewCount()).isZero();
    }
}

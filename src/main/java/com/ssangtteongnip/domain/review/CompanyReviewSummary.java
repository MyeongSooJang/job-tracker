package com.ssangtteongnip.domain.review;

// 한 회사에 작성된 후기의 개수와 평균 평점. 후기가 없으면 평균은 null이다.
public record CompanyReviewSummary(Long companyId, long reviewCount, Double averageRating) {

    public static CompanyReviewSummary none(Long companyId) {
        return new CompanyReviewSummary(companyId, 0, null);
    }
}

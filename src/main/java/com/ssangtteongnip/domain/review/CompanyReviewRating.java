package com.ssangtteongnip.domain.review;

// 한 회사에 작성된 후기의 개수와 평균 평점. 후기가 없으면 평균은 null이다.
public record CompanyReviewRating(Long companyId, long reviewCount, Double averageRating) {

    public static CompanyReviewRating none(Long companyId) {
        return new CompanyReviewRating(companyId, 0, null);
    }
}

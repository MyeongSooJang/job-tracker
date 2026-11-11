package com.ssangtteongnip.domain.review;

import com.ssangtteongnip.domain.company.Company;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 회사별 후기 평점 모음. 후기가 없어 집계에 나오지 않은 회사는 후기 0개인 평점을 돌려준다.
public class CompanyReviewRatings {

    private final Map<Long, CompanyReviewRating> ratingByCompanyId;

    public CompanyReviewRatings(List<CompanyReviewRating> ratings) {
        this.ratingByCompanyId = ratings.stream()
                .collect(Collectors.toMap(CompanyReviewRating::companyId, Function.identity()));
    }

    public CompanyReviewRating findCompanyRating(Company company) {
        return ratingByCompanyId.getOrDefault(company.getId(), CompanyReviewRating.none(company.getId()));
    }
}

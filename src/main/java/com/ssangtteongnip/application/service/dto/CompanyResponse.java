package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.review.CompanyReviewRating;

import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String name,
        String industry,
        String website,
        LocalDateTime createdAt,
        long reviewCount,
        Double averageRating) {

    public static CompanyResponse attachRating(Company company, CompanyReviewRating rating) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getIndustry(),
                company.getWebsite(),
                company.getCreatedAt(),
                rating.reviewCount(),
                rating.averageRating());
    }
}

package com.jobtracker.application.service.dto;

import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.review.CompanyReviewSummary;

import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String name,
        String industry,
        String website,
        LocalDateTime createdAt,
        long reviewCount,
        Double averageRating) {

    public static CompanyResponse of(Company company, CompanyReviewSummary summary) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getIndustry(),
                company.getWebsite(),
                company.getCreatedAt(),
                summary.reviewCount(),
                summary.averageRating());
    }
}

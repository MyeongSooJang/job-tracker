package com.jobtracker.application.service.dto;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.review.Review;

import java.time.LocalDateTime;

// 다른 사용자에게 공개되는 후기. 작성자를 식별할 수 있는 정보(userId 등)는 담지 않는다.
public record CompanyReviewResponse(
        Long id,
        int rating,
        String content,
        String position,
        ApplicationStatus stage,
        LocalDateTime createdAt) {

    public static CompanyReviewResponse from(Review review) {
        Application application = review.getApplication();
        return new CompanyReviewResponse(
                review.getId(),
                review.getRating(),
                review.getContent(),
                application.getPosition(),
                application.getStatus(),
                review.getCreatedAt());
    }
}

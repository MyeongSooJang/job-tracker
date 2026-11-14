package com.ssangtteongnip.domain.review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {
    Review save(Review review);
    Optional<Review> findById(Long id);
    List<Review> findByApplicationUserId(Long userId);
    boolean existsByApplicationId(Long applicationId);

    List<Review> findNewestRatedReviews(Long companyId);
    List<CompanyReviewRating> findRatings();
    Optional<CompanyReviewRating> findRating(Long companyId);
}

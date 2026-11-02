package com.jobtracker.domain.review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {
    Review save(Review review);
    Optional<Review> findById(Long id);
    List<Review> findByApplicationUserId(Long userId);
    boolean existsByApplicationId(Long applicationId);

    // 작성된(평점이 있는) 후기만, 최신순
    List<Review> findWrittenByCompanyId(Long companyId);
    List<CompanyReviewSummary> summarizeByCompany();
    Optional<CompanyReviewSummary> summarizeByCompanyId(Long companyId);
}

package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.review.Review;
import com.jobtracker.domain.review.ReviewRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewJpaRepository extends JpaRepository<Review, Long>, ReviewRepository {
    boolean existsByApplicationId(Long applicationId);

    List<Review> findByApplicationUserId(Long userId);
}

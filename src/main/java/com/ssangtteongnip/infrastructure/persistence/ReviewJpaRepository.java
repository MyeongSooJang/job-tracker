package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.review.CompanyReviewRating;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.domain.review.ReviewRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewJpaRepository extends JpaRepository<Review, Long>, ReviewRepository {
    boolean existsByApplicationId(Long applicationId);

    List<Review> findByApplicationUserId(Long userId);

    @Query("""
            select r from Review r join fetch r.application a
            where a.company.id = :companyId and r.rating is not null
            order by r.createdAt desc, r.id desc
            """)
    List<Review> findWrittenByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            select new com.ssangtteongnip.domain.review.CompanyReviewRating(a.company.id, count(r), avg(r.rating))
            from Review r join r.application a
            where r.rating is not null
            group by a.company.id
            """)
    List<CompanyReviewRating> calculateRatingByCompany();

    @Query("""
            select new com.ssangtteongnip.domain.review.CompanyReviewRating(a.company.id, count(r), avg(r.rating))
            from Review r join r.application a
            where a.company.id = :companyId and r.rating is not null
            group by a.company.id
            """)
    Optional<CompanyReviewRating> calculateRatingByCompanyId(@Param("companyId") Long companyId);
}

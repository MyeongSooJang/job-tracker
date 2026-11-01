package com.jobtracker.application.service;

import com.jobtracker.application.service.dto.ReviewRequest;
import com.jobtracker.domain.review.Review;
import com.jobtracker.domain.review.ReviewRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public List<Review> findAll(Long userId) {
        return reviewRepository.findByApplicationUserId(userId);
    }

    @Transactional
    public Review write(Long userId, Long id, ReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        review.verifyOwner(userId);
        review.write(request.content(), request.rating());
        return review;
    }
}

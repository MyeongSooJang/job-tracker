package com.jobtracker.application;

import com.jobtracker.application.service.ReviewService;
import com.jobtracker.application.service.dto.ReviewRequest;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.review.Review;
import com.jobtracker.domain.review.ReviewRepository;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long REVIEW_ID = 10L;

    @Mock ReviewRepository reviewRepository;
    @InjectMocks ReviewService reviewService;

    private Review review() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return Review.create(Application.create(OWNER_ID, company, "백엔드 개발자", LocalDate.of(2026, 11, 1)));
    }

    @Test
    void 소유자는_후기를_작성할_수_있다() {
        Review review = review();
        given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

        reviewService.write(OWNER_ID, REVIEW_ID, new ReviewRequest("분위기가 좋았다", 4));

        assertThat(review.getContent()).isEqualTo("분위기가 좋았다");
        assertThat(review.getRating()).isEqualTo(4);
    }

    @Test
    void 존재하지_않는_후기는_작성할_수_없다() {
        given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> reviewService.write(OWNER_ID, REVIEW_ID, new ReviewRequest("내용", 4)),
                ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    void 다른_사용자의_후기는_작성할_수_없다() {
        Review review = review();
        given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

        assertErrorCode(() -> reviewService.write(OTHER_USER_ID, REVIEW_ID, new ReviewRequest("내용", 4)),
                ErrorCode.REVIEW_NOT_FOUND);
        assertThat(review.getContent()).isNull();
        assertThat(review.getRating()).isNull();
    }
}

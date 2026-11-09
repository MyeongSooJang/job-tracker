package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.ReviewService;
import com.ssangtteongnip.application.service.dto.ReviewRequest;
import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.review.ApplyRoute;
import com.ssangtteongnip.domain.review.InterviewAtmosphere;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.domain.review.ReviewRepository;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
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

        reviewService.write(OWNER_ID, REVIEW_ID, request("분위기가 좋았다", 4));

        assertThat(review.getContent()).isEqualTo("분위기가 좋았다");
        assertThat(review.getRating()).isEqualTo(4);
    }

    @Test
    void 후기를_작성하면_면접_상세도_함께_저장된다() {
        Review review = review();
        given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

        reviewService.write(OWNER_ID, REVIEW_ID, new ReviewRequest("분위기가 좋았다", 4,
                3, InterviewAtmosphere.COMFORTABLE, ApplyRoute.ONLINE, List.of("자기소개")));

        assertThat(review.getDifficulty()).isEqualTo(3);
        assertThat(review.getAtmosphere()).isEqualTo(InterviewAtmosphere.COMFORTABLE);
        assertThat(review.getRoute()).isEqualTo(ApplyRoute.ONLINE);
        assertThat(review.getQuestions()).containsExactly("자기소개");
    }

    @Test
    void 존재하지_않는_후기는_작성할_수_없다() {
        given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> reviewService.write(OWNER_ID, REVIEW_ID, request("내용", 4)),
                ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    void 다른_사용자의_후기는_작성할_수_없다() {
        Review review = review();
        given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

        assertErrorCode(() -> reviewService.write(OTHER_USER_ID, REVIEW_ID, new ReviewRequest("내용", 4,
                        3, InterviewAtmosphere.TENSE, ApplyRoute.OTHER, List.of("자기소개"))),
                ErrorCode.REVIEW_NOT_FOUND);
        assertThat(review.getContent()).isNull();
        assertThat(review.getRating()).isNull();
        assertThat(review.getDifficulty()).isNull();
        assertThat(review.getQuestions()).isEmpty();
    }

    private ReviewRequest request(String content, int rating) {
        return new ReviewRequest(content, rating, null, null, null, null);
    }
}

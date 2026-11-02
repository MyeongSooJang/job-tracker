package com.jobtracker.domain;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.review.Review;
import com.jobtracker.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static com.jobtracker.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ReviewTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private Review review() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return Review.create(Application.create(OWNER_ID, company, "백엔드 개발자", LocalDate.of(2026, 11, 1)));
    }

    @Test
    void 후기와_평점을_작성하면_내용이_저장된다() {
        Review review = review();

        review.write("분위기가 좋았다", 4);

        assertThat(review.getContent()).isEqualTo("분위기가 좋았다");
        assertThat(review.getRating()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void 평점은_1점부터_5점까지_허용한다(int rating) {
        Review review = review();

        review.write("내용", rating);

        assertThat(review.getRating()).isEqualTo(rating);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void 평점이_1점_미만이거나_5점_초과면_작성할_수_없다(int rating) {
        Review review = review();

        assertErrorCode(() -> review.write("내용", rating), ErrorCode.INVALID_RATING);
        assertThat(review.getRating()).isNull();
    }

    @Test
    void 소유자는_소유권_검증을_통과한다() {
        Review review = review();

        assertThatCode(() -> review.verifyOwner(OWNER_ID)).doesNotThrowAnyException();
    }

    @Test
    void 다른_사용자의_후기는_존재하지_않는_것으로_취급한다() {
        Review review = review();

        assertErrorCode(() -> review.verifyOwner(OTHER_USER_ID), ErrorCode.REVIEW_NOT_FOUND);
    }
}

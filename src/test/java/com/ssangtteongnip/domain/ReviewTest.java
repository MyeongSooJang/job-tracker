package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.review.ApplyRoute;
import com.ssangtteongnip.domain.review.InterviewAtmosphere;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
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
    void 면접_상세를_기록하면_난이도_분위기_경로_질문이_저장된다() {
        Review review = review();

        review.describeInterview(4, InterviewAtmosphere.TENSE, ApplyRoute.REFERRAL,
                List.of("자기소개", "트랜잭션 격리 수준"));

        assertThat(review.getDifficulty()).isEqualTo(4);
        assertThat(review.getAtmosphere()).isEqualTo(InterviewAtmosphere.TENSE);
        assertThat(review.getRoute()).isEqualTo(ApplyRoute.REFERRAL);
        assertThat(review.getQuestions()).containsExactly("자기소개", "트랜잭션 격리 수준");
    }

    @Test
    void 면접_상세는_모두_비워도_기록할_수_있다() {
        Review review = review();

        review.describeInterview(null, null, null, null);

        assertThat(review.getDifficulty()).isNull();
        assertThat(review.getAtmosphere()).isNull();
        assertThat(review.getRoute()).isNull();
        assertThat(review.getQuestions()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void 난이도는_1점부터_5점까지_허용한다(int difficulty) {
        Review review = review();

        review.describeInterview(difficulty, null, null, List.of());

        assertThat(review.getDifficulty()).isEqualTo(difficulty);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void 난이도가_1점_미만이거나_5점_초과면_기록할_수_없다(int difficulty) {
        Review review = review();

        assertErrorCode(() -> review.describeInterview(difficulty, null, null, List.of()),
                ErrorCode.INVALID_DIFFICULTY);
        assertThat(review.getDifficulty()).isNull();
    }

    @Test
    void 질문은_앞뒤_공백을_지우고_빈_질문은_버린다() {
        Review review = review();

        review.describeInterview(null, null, null, Arrays.asList("  자기소개  ", "", "   ", null, "프로젝트 경험"));

        assertThat(review.getQuestions()).containsExactly("자기소개", "프로젝트 경험");
    }

    @Test
    void 면접_상세를_다시_기록하면_이전_질문은_교체된다() {
        Review review = review();
        review.describeInterview(3, null, null, List.of("자기소개"));

        review.describeInterview(2, null, null, List.of("프로젝트 경험"));

        assertThat(review.getDifficulty()).isEqualTo(2);
        assertThat(review.getQuestions()).containsExactly("프로젝트 경험");
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

    @Test
    void 후기를_작성하면_평점과_면접_상세가_함께_저장된다() {
        Review review = review();

        review.compose("분위기가 좋았다", 4, 3, InterviewAtmosphere.COMFORTABLE, ApplyRoute.REFERRAL, List.of("자기소개"));

        assertThat(review.getContent()).isEqualTo("분위기가 좋았다");
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getDifficulty()).isEqualTo(3);
        assertThat(review.getQuestions()).containsExactly("자기소개");
    }

    @Test
    void 면접_난이도가_잘못되면_평점과_내용도_저장되지_않는다() {
        Review review = review();

        assertErrorCode(() -> review.compose("내용", 4, 6, null, null, List.of()), ErrorCode.INVALID_DIFFICULTY);

        assertThat(review.getContent()).isNull();
        assertThat(review.getRating()).isNull();
    }

    @Test
    void 평점이_잘못되면_면접_상세도_저장되지_않는다() {
        Review review = review();

        assertErrorCode(() -> review.compose("내용", 0, 3, null, null, List.of("자기소개")), ErrorCode.INVALID_RATING);

        assertThat(review.getDifficulty()).isNull();
        assertThat(review.getQuestions()).isEmpty();
    }
}

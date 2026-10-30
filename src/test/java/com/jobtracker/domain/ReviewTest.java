package com.jobtracker.domain;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.review.Review;
import com.jobtracker.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewTest {

    private Review review() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return Review.create(Application.create(1L, company, "백엔드 개발자", LocalDate.now()));
    }

    @Test
    void write_후기와_평점을_작성한다() {
        Review review = review();

        review.write("분위기가 좋았다", 4);

        assertThat(review.getContent()).isEqualTo("분위기가 좋았다");
        assertThat(review.getRating()).isEqualTo(4);
    }

    @Test
    void write_평점이_1_미만이거나_5_초과면_예외가_발생한다() {
        Review review = review();

        assertThatThrownBy(() -> review.write("내용", 0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> review.write("내용", 6)).isInstanceOf(BusinessException.class);
    }
}

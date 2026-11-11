package com.ssangtteongnip.domain.review;

import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Review {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer rating;

    private Integer difficulty;

    @Enumerated(EnumType.STRING)
    private InterviewAtmosphere atmosphere;

    @Enumerated(EnumType.STRING)
    private ApplyRoute route;

    // 컨트롤러가 엔티티를 직렬화하므로 트랜잭션 밖에서도 읽히도록 EAGER로 둔다.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_questions", joinColumns = @JoinColumn(name = "review_id"))
    @OrderColumn(name = "question_order")
    @Column(name = "question", columnDefinition = "TEXT")
    private List<String> questions = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static Review create(Application application) {
        Review review = new Review();
        review.application = application;
        return review;
    }

    public void verifyOwner(Long userId) {
        if (!application.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }
    }

    // 후기 작성 한 번에 평점·내용과 면접 상세를 함께 확정한다. 어느 하나라도 잘못되면 아무것도 바꾸지 않는다.
    public void compose(String content, int rating, Integer difficulty, InterviewAtmosphere atmosphere,
                        ApplyRoute route, List<String> questions) {
        requireValidRating(rating);
        requireValidDifficulty(difficulty);
        write(content, rating);
        describeInterview(difficulty, atmosphere, route, questions);
    }

    public void write(String content, int rating) {
        requireValidRating(rating);
        this.content = content;
        this.rating = rating;
    }

    // 면접 상세는 전부 선택값이다. 다시 기록하면 이전 값은 통째로 교체된다.
    public void describeInterview(Integer difficulty, InterviewAtmosphere atmosphere,
                                  ApplyRoute route, List<String> questions) {
        requireValidDifficulty(difficulty);
        this.difficulty = difficulty;
        this.atmosphere = atmosphere;
        this.route = route;
        this.questions.clear();
        if (questions != null) {
            questions.stream()
                    .filter(Objects::nonNull)
                    .map(String::strip)
                    .filter(question -> !question.isEmpty())
                    .forEach(this.questions::add);
        }
    }

    private static void requireValidRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new BusinessException(ErrorCode.INVALID_RATING);
        }
    }

    private static void requireValidDifficulty(Integer difficulty) {
        if (difficulty != null && (difficulty < MIN_DIFFICULTY || difficulty > MAX_DIFFICULTY)) {
            throw new BusinessException(ErrorCode.INVALID_DIFFICULTY);
        }
    }
}

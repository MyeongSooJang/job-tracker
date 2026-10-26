package com.jobtracker.domain.interview;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interview_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private Interview interview;

    @Column(nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private String memo;

    static InterviewQuestion create(Interview interview, String question, String answer, String memo) {
        InterviewQuestion q = new InterviewQuestion();
        q.interview = interview;
        q.question = question;
        q.answer = answer;
        q.memo = memo;
        return q;
    }
}

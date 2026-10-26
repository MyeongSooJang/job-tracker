package com.jobtracker.domain.interview;

import com.jobtracker.domain.application.Application;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "interviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id")
    private Application application;

    private int round;

    private LocalDateTime interviewDate;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewQuestion> questions = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Interview create(Application application, int round,
                                   LocalDateTime interviewDate, String location, String note) {
        Interview interview = new Interview();
        interview.application = application;
        interview.round = round;
        interview.interviewDate = interviewDate;
        interview.location = location;
        interview.note = note;
        return interview;
    }

    public void addQuestion(String question, String answer, String memo) {
        questions.add(InterviewQuestion.create(this, question, answer, memo));
    }

    public List<InterviewQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }
}

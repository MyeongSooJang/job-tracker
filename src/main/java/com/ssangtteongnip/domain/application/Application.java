package com.ssangtteongnip.domain.application;

import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    private LocalDate appliedDate;

    // 기존 지원 건은 사이트를 몰라 null일 수 있다.
    @Enumerated(EnumType.STRING)
    private JobSite jobSite;

    // 공고 마감일. 모르면 null이고, null이면 자동 불합격 대상이 아니다.
    private LocalDate deadline;

    // 기존 지원 건은 컬럼이 추가될 때 NOT_VIEWED로 채워진다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) not null default 'NOT_VIEWED'")
    private ResumeView resumeView;

    // 마감일 + 1달이 지나 자동으로 불합격이 된 시점. 직접 불합격 처리했거나 불합격이 아니면 null이다.
    private LocalDateTime autoRejectedAt;

    // 서류 통과(면접 일정이나 면접 기록이 처음 등록된) 시점. 이후 불합격이 되어도 유지되며, 서류 탈락이면 null이다.
    private LocalDateTime documentPassedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static Application create(Long userId, Company company, String position, LocalDate appliedDate) {
        return create(userId, company, position, appliedDate, null);
    }

    public static Application create(Long userId, Company company, String position, LocalDate appliedDate,
                                     JobSite jobSite) {
        return create(userId, company, position, appliedDate, jobSite, null, ResumeView.NOT_VIEWED);
    }

    public static Application create(Long userId, Company company, String position, LocalDate appliedDate,
                                     JobSite jobSite, LocalDate deadline, ResumeView resumeView) {
        Application application = new Application();
        application.jobSite = jobSite;
        application.userId = userId;
        application.company = company;
        application.position = position;
        application.status = ApplicationStatus.RESUME_SUBMITTED;
        application.appliedDate = appliedDate;
        application.deadline = deadline;
        application.resumeView = Objects.requireNonNull(resumeView);
        return application;
    }

    public boolean isOwnedBy(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    public void verifyOwner(Long userId) {
        if (!isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
    }

    public void changeJobSite(JobSite jobSite) {
        this.jobSite = Objects.requireNonNull(jobSite);
    }

    public void changeDeadline(LocalDate deadline) {
        this.deadline = Objects.requireNonNull(deadline);
    }

    public void changeResumeView(ResumeView resumeView) {
        this.resumeView = Objects.requireNonNull(resumeView);
    }

    public boolean isDocumentPassed() {
        return documentPassedAt != null;
    }

    // 면접 일정이나 면접 기록이 등록됐을 때 호출한다. 서류 제출 상태일 때만 면접으로 전이하며 서류 통과 시점을 한 번만 기록한다.
    public void startInterview(LocalDateTime now) {
        if (this.status.canAdvanceTo(ApplicationStatus.INTERVIEW)) {
            this.status = ApplicationStatus.INTERVIEW;
            this.documentPassedAt = now;
        }
    }

    // 최종 합격. 면접 중인 지원만 합격할 수 있다.
    public void accept() {
        moveTo(ApplicationStatus.ACCEPTED);
    }

    // 불합격. 서류 탈락(서류 제출 상태)과 면접 후 불합격(면접 상태) 모두 가능하다.
    public void reject() {
        moveTo(ApplicationStatus.REJECTED);
    }

    private void moveTo(ApplicationStatus next) {
        if (!this.status.canAdvanceTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = next;
    }
}

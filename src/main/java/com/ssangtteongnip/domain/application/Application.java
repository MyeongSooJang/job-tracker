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
        Application application = new Application();
        application.jobSite = jobSite;
        application.userId = userId;
        application.company = company;
        application.position = position;
        application.status = ApplicationStatus.RESUME_SUBMITTED;
        application.appliedDate = appliedDate;
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

    public void advanceStatus(ApplicationStatus next) {
        if (!this.status.canAdvanceTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = next;
    }
}

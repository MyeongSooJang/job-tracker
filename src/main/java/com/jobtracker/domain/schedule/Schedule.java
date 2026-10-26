package com.jobtracker.domain.schedule;

import com.jobtracker.domain.application.Application;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType type;

    private String note;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Schedule create(Application application, String title,
                                  LocalDateTime scheduledAt, ScheduleType type, String note) {
        Schedule schedule = new Schedule();
        schedule.application = application;
        schedule.title = title;
        schedule.scheduledAt = scheduledAt;
        schedule.type = type;
        schedule.note = note;
        return schedule;
    }
}

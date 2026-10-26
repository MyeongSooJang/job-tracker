package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleJpaRepository extends JpaRepository<Schedule, Long>, ScheduleRepository {
    List<Schedule> findByScheduledAtBetween(LocalDateTime from, LocalDateTime to);
}

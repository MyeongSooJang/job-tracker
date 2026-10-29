package com.jobtracker.domain.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    Schedule save(Schedule schedule);
    Optional<Schedule> findById(Long id);
    List<Schedule> findAll();
    List<Schedule> findByScheduledAtBetween(LocalDateTime from, LocalDateTime to);
}

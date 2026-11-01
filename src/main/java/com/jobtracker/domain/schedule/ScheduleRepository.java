package com.jobtracker.domain.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    Schedule save(Schedule schedule);
    Optional<Schedule> findById(Long id);
    List<Schedule> findByApplicationUserId(Long userId);
    List<Schedule> findByApplicationUserIdAndScheduledAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
}

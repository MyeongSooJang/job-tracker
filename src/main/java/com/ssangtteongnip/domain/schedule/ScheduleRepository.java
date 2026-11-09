package com.ssangtteongnip.domain.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    Schedule save(Schedule schedule);
    Optional<Schedule> findById(Long id);
    List<Schedule> findByApplicationUserId(Long userId);

    // 소유한 지원 건의 일정 중 from~to(양 끝 포함)에 잡힌 것
    List<Schedule> findInPeriod(Long userId, LocalDateTime from, LocalDateTime to);
}

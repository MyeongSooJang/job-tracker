package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleJpaRepository extends JpaRepository<Schedule, Long>, ScheduleRepository {
    List<Schedule> findByApplicationUserId(Long userId);

    @Override
    @Query("select s from Schedule s where s.application.userId = :userId "
            + "and s.scheduledAt between :from and :to")
    List<Schedule> findInPeriod(@Param("userId") Long userId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}

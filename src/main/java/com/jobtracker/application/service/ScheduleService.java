package com.jobtracker.application.service;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleRepository;
import com.jobtracker.application.service.dto.ScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ApplicationService applicationService;

    @Transactional
    public Schedule register(ScheduleRequest request) {
        Application application = applicationService.findById(request.applicationId());
        return scheduleRepository.save(
                Schedule.create(application, request.title(), request.scheduledAt(),
                        request.type(), request.address(), request.note())
        );
    }

    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }

    public List<Schedule> findByPeriod(LocalDateTime from, LocalDateTime to) {
        return scheduleRepository.findByScheduledAtBetween(from, to);
    }
}

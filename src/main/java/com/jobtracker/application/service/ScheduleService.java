package com.jobtracker.application.service;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.review.Review;
import com.jobtracker.domain.review.ReviewRepository;
import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.domain.schedule.ScheduleType;
import com.jobtracker.domain.schedule.ScheduleRepository;
import com.jobtracker.application.service.dto.ScheduleAddressRequest;
import com.jobtracker.application.service.dto.ScheduleRequest;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
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
    private final ReviewRepository reviewRepository;

    @Transactional
    public Schedule register(ScheduleRequest request) {
        Application application = applicationService.findById(request.applicationId());
        Schedule schedule = scheduleRepository.save(
                Schedule.create(application, request.title(), request.scheduledAt(),
                        request.type(), request.address(), request.note())
        );
        if (request.type() == ScheduleType.INTERVIEW
                && !reviewRepository.existsByApplicationId(application.getId())) {
            reviewRepository.save(Review.create(application));
        }
        return schedule;
    }

    @Transactional
    public Schedule updateAddress(Long id, ScheduleAddressRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        schedule.changeAddress(request.address());
        return schedule;
    }

    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }

    public List<Schedule> findByPeriod(LocalDateTime from, LocalDateTime to) {
        return scheduleRepository.findByScheduledAtBetween(from, to);
    }
}

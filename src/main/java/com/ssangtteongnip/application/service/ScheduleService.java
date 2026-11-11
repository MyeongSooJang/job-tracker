package com.ssangtteongnip.application.service;

import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.domain.review.ReviewRepository;
import com.ssangtteongnip.domain.schedule.Schedule;
import com.ssangtteongnip.domain.schedule.ScheduleRepository;
import com.ssangtteongnip.application.service.dto.ScheduleAddressRequest;
import com.ssangtteongnip.application.service.dto.ScheduleRequest;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ApplicationService applicationService;
    private final ReviewRepository reviewRepository;
    private final Clock clock;

    @Transactional
    public Schedule register(Long userId, ScheduleRequest request) {
        Application application = applicationService.findById(userId, request.applicationId());
        Schedule schedule = scheduleRepository.save(
                Schedule.create(application, request.title(), request.scheduledAt(),
                        request.type(), request.address(), request.note())
        );
        if (schedule.isInterview()) {
            application.startInterview(LocalDateTime.now(clock));
            createReviewIfAbsent(application);
        }
        return schedule;
    }

    private void createReviewIfAbsent(Application application) {
        if (!reviewRepository.existsByApplicationId(application.getId())) {
            reviewRepository.save(Review.create(application));
        }
    }

    @Transactional
    public Schedule updateAddress(Long userId, Long id, ScheduleAddressRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        schedule.verifyOwner(userId);
        schedule.changeAddress(request.address());
        return schedule;
    }

    public List<Schedule> findAll(Long userId) {
        return scheduleRepository.findByApplicationUserId(userId);
    }

    public List<Schedule> findByPeriod(Long userId, LocalDateTime from, LocalDateTime to) {
        return scheduleRepository.findInPeriod(userId, from, to);
    }
}

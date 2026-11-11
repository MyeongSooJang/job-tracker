package com.ssangtteongnip.application.service;

import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.interview.Interview;
import com.ssangtteongnip.domain.interview.InterviewRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.application.service.dto.InterviewQuestionRequest;
import com.ssangtteongnip.application.service.dto.InterviewRequest;
import com.ssangtteongnip.application.service.dto.InterviewScoreRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationService applicationService;
    private final Clock clock;

    @Transactional
    public Interview register(Long userId, Long applicationId, InterviewRequest request) {
        Application application = applicationService.findById(userId, applicationId);
        Interview interview = Interview.create(application, request.round(), request.interviewDate(),
                request.location(), request.note());
        interview.evaluate(request.score());
        application.startInterview(LocalDateTime.now(clock));
        return interviewRepository.save(interview);
    }

    @Transactional
    public Interview evaluate(Long userId, Long applicationId, Long interviewId, InterviewScoreRequest request) {
        Interview interview = findInterview(userId, applicationId, interviewId);
        interview.evaluate(request.score());
        return interview;
    }

    public List<Interview> findByApplicationId(Long userId, Long applicationId) {
        applicationService.findById(userId, applicationId);
        return interviewRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public Interview addQuestion(Long userId, Long applicationId, Long interviewId, InterviewQuestionRequest request) {
        Interview interview = findInterview(userId, applicationId, interviewId);
        interview.addQuestion(request.question(), request.answer(), request.memo());
        return interview;
    }

    private Interview findInterview(Long userId, Long applicationId, Long interviewId) {
        applicationService.findById(userId, applicationId);
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
        if (!interview.getApplication().getId().equals(applicationId)) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
        }
        return interview;
    }
}

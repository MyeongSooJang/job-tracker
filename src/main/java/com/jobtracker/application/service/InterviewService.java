package com.jobtracker.application.service;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.interview.Interview;
import com.jobtracker.domain.interview.InterviewRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.application.service.dto.InterviewQuestionRequest;
import com.jobtracker.application.service.dto.InterviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationService applicationService;

    @Transactional
    public Interview register(Long applicationId, InterviewRequest request) {
        Application application = applicationService.findById(applicationId);
        return interviewRepository.save(
                Interview.create(application, request.round(), request.interviewDate(),
                        request.location(), request.note())
        );
    }

    public List<Interview> findByApplicationId(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public Interview addQuestion(Long applicationId, Long interviewId, InterviewQuestionRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
        if (!interview.getApplication().getId().equals(applicationId)) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
        }
        interview.addQuestion(request.question(), request.answer(), request.memo());
        return interview;
    }
}

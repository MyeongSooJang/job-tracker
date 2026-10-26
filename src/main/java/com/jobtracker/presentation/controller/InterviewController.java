package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.InterviewService;
import com.jobtracker.domain.interview.Interview;
import com.jobtracker.application.service.dto.InterviewQuestionRequest;
import com.jobtracker.application.service.dto.InterviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Interview register(@PathVariable Long applicationId,
                              @RequestBody @Valid InterviewRequest request) {
        return interviewService.register(applicationId, request);
    }

    @GetMapping
    public List<Interview> findAll(@PathVariable Long applicationId) {
        return interviewService.findByApplicationId(applicationId);
    }

    @PostMapping("/{interviewId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Interview addQuestion(@PathVariable Long applicationId,
                                 @PathVariable Long interviewId,
                                 @RequestBody @Valid InterviewQuestionRequest request) {
        return interviewService.addQuestion(applicationId, interviewId, request);
    }
}

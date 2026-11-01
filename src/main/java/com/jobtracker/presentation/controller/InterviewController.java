package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.InterviewService;
import com.jobtracker.domain.interview.Interview;
import com.jobtracker.application.service.dto.InterviewQuestionRequest;
import com.jobtracker.application.service.dto.InterviewRequest;
import com.jobtracker.application.service.dto.InterviewScoreRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Interview register(@AuthenticationPrincipal Long userId,
                              @PathVariable Long applicationId,
                              @RequestBody @Valid InterviewRequest request) {
        return interviewService.register(userId, applicationId, request);
    }

    @GetMapping
    public List<Interview> findAll(@AuthenticationPrincipal Long userId, @PathVariable Long applicationId) {
        return interviewService.findByApplicationId(userId, applicationId);
    }

    @PatchMapping("/{interviewId}/score")
    public Interview evaluate(@AuthenticationPrincipal Long userId,
                              @PathVariable Long applicationId,
                              @PathVariable Long interviewId,
                              @RequestBody @Valid InterviewScoreRequest request) {
        return interviewService.evaluate(userId, applicationId, interviewId, request);
    }

    @PostMapping("/{interviewId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Interview addQuestion(@AuthenticationPrincipal Long userId,
                                 @PathVariable Long applicationId,
                                 @PathVariable Long interviewId,
                                 @RequestBody @Valid InterviewQuestionRequest request) {
        return interviewService.addQuestion(userId, applicationId, interviewId, request);
    }
}

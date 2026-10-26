package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.application.service.dto.ApplicationRequest;
import com.jobtracker.application.service.dto.ApplicationStatusRequest;
import com.jobtracker.domain.application.Application;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Application register(@RequestBody @Valid ApplicationRequest request) {
        return applicationService.register(request);
    }

    @GetMapping
    public List<Application> findAll() {
        return applicationService.findAll();
    }

    @GetMapping("/{id}")
    public Application findById(@PathVariable Long id) {
        return applicationService.findById(id);
    }

    @PatchMapping("/{id}/status")
    public Application advanceStatus(@PathVariable Long id,
                                     @RequestBody @Valid ApplicationStatusRequest request) {
        return applicationService.advanceStatus(id, request);
    }
}

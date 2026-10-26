package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.ScheduleService;
import com.jobtracker.domain.schedule.Schedule;
import com.jobtracker.application.service.dto.ScheduleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Schedule register(@RequestBody @Valid ScheduleRequest request) {
        return scheduleService.register(request);
    }

    @GetMapping
    public List<Schedule> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from != null && to != null) {
            return scheduleService.findByPeriod(from, to);
        }
        return scheduleService.findAll();
    }
}

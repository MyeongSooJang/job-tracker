package com.jobtracker.presentation.controller;

import com.jobtracker.application.service.ReviewService;
import com.jobtracker.application.service.dto.ReviewRequest;
import com.jobtracker.domain.review.Review;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public List<Review> findAll() {
        return reviewService.findAll();
    }

    @PatchMapping("/{id}")
    public Review write(@PathVariable Long id, @RequestBody @Valid ReviewRequest request) {
        return reviewService.write(id, request);
    }
}

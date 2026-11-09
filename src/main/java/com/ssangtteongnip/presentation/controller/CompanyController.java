package com.ssangtteongnip.presentation.controller;

import com.ssangtteongnip.application.service.CompanyService;
import com.ssangtteongnip.application.service.dto.CompanyRequest;
import com.ssangtteongnip.application.service.dto.CompanyResponse;
import com.ssangtteongnip.application.service.dto.CompanyReviewResponse;
import com.ssangtteongnip.domain.company.Company;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Company register(@RequestBody @Valid CompanyRequest request) {
        return companyService.register(request);
    }

    @GetMapping
    public List<CompanyResponse> findAll() {
        return companyService.findAll();
    }

    @GetMapping("/{id}")
    public CompanyResponse findById(@PathVariable Long id) {
        return companyService.findDetail(id);
    }

    @GetMapping("/{id}/reviews")
    public List<CompanyReviewResponse> findReviews(@PathVariable Long id) {
        return companyService.findReviews(id);
    }
}

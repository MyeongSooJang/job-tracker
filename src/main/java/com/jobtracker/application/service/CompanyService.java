package com.jobtracker.application.service;

import com.jobtracker.application.service.dto.CompanyRequest;
import com.jobtracker.application.service.dto.CompanyResponse;
import com.jobtracker.application.service.dto.CompanyReviewResponse;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.company.CompanyRepository;
import com.jobtracker.domain.review.CompanyReviewSummary;
import com.jobtracker.domain.review.ReviewRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public Company register(CompanyRequest request) {
        if (companyRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.COMPANY_ALREADY_EXISTS);
        }
        return companyRepository.save(
                Company.create(request.name(), request.industry(), request.website())
        );
    }

    public List<CompanyResponse> findAll() {
        Map<Long, CompanyReviewSummary> summaries = reviewRepository.summarizeByCompany().stream()
                .collect(Collectors.toMap(CompanyReviewSummary::companyId, Function.identity()));
        return companyRepository.findAll().stream()
                .map(company -> CompanyResponse.of(company,
                        summaries.getOrDefault(company.getId(), CompanyReviewSummary.none(company.getId()))))
                .toList();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }

    public CompanyResponse findDetail(Long id) {
        Company company = findById(id);
        CompanyReviewSummary summary = reviewRepository.summarizeByCompanyId(id)
                .orElse(CompanyReviewSummary.none(id));
        return CompanyResponse.of(company, summary);
    }

    public List<CompanyReviewResponse> findReviews(Long id) {
        findById(id);
        return reviewRepository.findWrittenByCompanyId(id).stream()
                .map(CompanyReviewResponse::from)
                .toList();
    }
}

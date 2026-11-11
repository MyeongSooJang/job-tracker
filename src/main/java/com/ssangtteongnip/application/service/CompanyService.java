package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.CompanyRequest;
import com.ssangtteongnip.application.service.dto.CompanyResponse;
import com.ssangtteongnip.application.service.dto.CompanyReviewResponse;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.company.CompanyRepository;
import com.ssangtteongnip.domain.review.CompanyReviewRating;
import com.ssangtteongnip.domain.review.ReviewRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
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
        Map<Long, CompanyReviewRating> ratings = reviewRepository.calculateRatingByCompany().stream()
                .collect(Collectors.toMap(CompanyReviewRating::companyId, Function.identity()));
        return companyRepository.findAll().stream()
                .map(company -> CompanyResponse.of(company,
                        ratings.getOrDefault(company.getId(), CompanyReviewRating.none(company.getId()))))
                .toList();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }

    public CompanyResponse findDetail(Long id) {
        Company company = findById(id);
        CompanyReviewRating rating = reviewRepository.calculateRatingByCompanyId(id)
                .orElse(CompanyReviewRating.none(id));
        return CompanyResponse.of(company, rating);
    }

    public List<CompanyReviewResponse> findReviews(Long id) {
        findById(id);
        return reviewRepository.findWrittenByCompanyId(id).stream()
                .map(CompanyReviewResponse::from)
                .toList();
    }
}

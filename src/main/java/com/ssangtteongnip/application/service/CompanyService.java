package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.CompanyRequest;
import com.ssangtteongnip.application.service.dto.CompanyResponse;
import com.ssangtteongnip.application.service.dto.CompanyReviewResponse;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.company.CompanyRepository;
import com.ssangtteongnip.domain.review.CompanyReviewRating;
import com.ssangtteongnip.domain.review.CompanyReviewRatings;
import com.ssangtteongnip.domain.review.ReviewRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        List<CompanyReviewRating> ratingsFromDb = reviewRepository.findRatings();
        CompanyReviewRatings reviewRatings = new CompanyReviewRatings(ratingsFromDb);
        List<Company> companies = companyRepository.findAll();

        return companies.stream()
                .map(company -> {
                    CompanyReviewRating rating = reviewRatings.findCompanyRating(company);
                    return CompanyResponse.attachRating(company, rating);
                })
                .toList();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }

    public CompanyResponse findDetail(Long id) {
        Company company = findById(id);
        CompanyReviewRating rating = reviewRepository.findRating(id)
                .orElse(CompanyReviewRating.none(id));
        return CompanyResponse.attachRating(company, rating);
    }

    public List<CompanyReviewResponse> findReviews(Long id) {
        findById(id);
        return reviewRepository.findNewestRatedReviews(id).stream()
                .map(CompanyReviewResponse::from)
                .toList();
    }
}

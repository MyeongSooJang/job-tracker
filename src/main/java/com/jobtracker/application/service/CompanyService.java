package com.jobtracker.application.service;

import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.company.CompanyRepository;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import com.jobtracker.application.service.dto.CompanyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public Company register(CompanyRequest request) {
        if (companyRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.COMPANY_ALREADY_EXISTS);
        }
        return companyRepository.save(
                Company.create(request.name(), request.industry(), request.website())
        );
    }

    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }
}

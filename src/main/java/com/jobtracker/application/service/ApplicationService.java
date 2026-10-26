package com.jobtracker.application.service;

import com.jobtracker.application.service.dto.ApplicationRequest;
import com.jobtracker.application.service.dto.ApplicationStatusRequest;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationRepository;
import com.jobtracker.domain.company.Company;
import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CompanyService companyService;

    @Transactional
    public Application register(ApplicationRequest request) {
        Company company = companyService.findById(request.companyId());
        // TODO: replace with authenticated userId when JWT auth is added
        return applicationRepository.save(
                Application.create(1L, company, request.position(), request.appliedDate())
        );
    }

    public List<Application> findAll() {
        return applicationRepository.findAll();
    }

    public Application findById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    @Transactional
    public Application advanceStatus(Long id, ApplicationStatusRequest request) {
        Application application = findById(id);
        application.advanceStatus(request.status());
        return application;
    }
}

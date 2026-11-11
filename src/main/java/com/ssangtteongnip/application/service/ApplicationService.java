package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.ApplicationRequest;
import com.ssangtteongnip.application.service.dto.ApplicationStatusRequest;
import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.application.ApplicationRepository;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
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
    public Application register(Long userId, ApplicationRequest request) {
        Company company = companyService.findById(request.companyId());
        return applicationRepository.save(
                Application.create(userId, company, request.position(), request.appliedDate(),
                        request.jobSite())
        );
    }

    public List<Application> findAll(Long userId) {
        return applicationRepository.findByUserId(userId);
    }

    public List<Application> findByStatus(Long userId, ApplicationStatus status) {
        return applicationRepository.findByUserIdAndStatus(userId, status);
    }

    public Application findById(Long userId, Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        application.verifyOwner(userId);
        return application;
    }

    @Transactional
    public Application advanceStatus(Long userId, Long id, ApplicationStatusRequest request) {
        if (request.status() == ApplicationStatus.INTERVIEW) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_REQUIRED);
        }
        Application application = findById(userId, id);
        application.advanceStatus(request.status());
        return application;
    }
}

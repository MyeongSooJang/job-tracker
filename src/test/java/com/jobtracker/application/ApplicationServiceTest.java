package com.jobtracker.application;

import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.application.service.CompanyService;
import com.jobtracker.application.service.dto.ApplicationStatusRequest;
import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationRepository;
import com.jobtracker.domain.application.ApplicationStatus;
import com.jobtracker.domain.company.Company;
import com.jobtracker.domain.review.Review;
import com.jobtracker.domain.review.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock CompanyService companyService;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks ApplicationService applicationService;

    private Application application() {
        Company company = Company.create(1L, "카카오", "IT", "https://kakao.com");
        return Application.create(1L, company, "백엔드 개발자", LocalDate.now());
    }

    @Test
    void advanceStatus_서류합격시_후기를_자동_생성한다() {
        given(applicationRepository.findById(1L)).willReturn(Optional.of(application()));
        given(reviewRepository.existsByApplicationId(1L)).willReturn(false);

        applicationService.advanceStatus(1L, new ApplicationStatusRequest(ApplicationStatus.INTERVIEW));

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void advanceStatus_이미_후기가_있으면_다시_생성하지_않는다() {
        given(applicationRepository.findById(1L)).willReturn(Optional.of(application()));
        given(reviewRepository.existsByApplicationId(1L)).willReturn(true);

        applicationService.advanceStatus(1L, new ApplicationStatusRequest(ApplicationStatus.INTERVIEW));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void advanceStatus_불합격시_후기를_생성하지_않는다() {
        given(applicationRepository.findById(1L)).willReturn(Optional.of(application()));

        applicationService.advanceStatus(1L, new ApplicationStatusRequest(ApplicationStatus.REJECTED));

        verify(reviewRepository, never()).save(any(Review.class));
    }
}

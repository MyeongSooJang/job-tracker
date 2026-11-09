package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.application.Application;
import com.ssangtteongnip.domain.application.ApplicationRepository;
import com.ssangtteongnip.domain.application.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationJpaRepository extends JpaRepository<Application, Long>, ApplicationRepository {
    List<Application> findByUserId(Long userId);

    List<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status);
}

package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationRepository;
import com.jobtracker.domain.application.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationJpaRepository extends JpaRepository<Application, Long>, ApplicationRepository {
    List<Application> findByStatus(ApplicationStatus status);
}

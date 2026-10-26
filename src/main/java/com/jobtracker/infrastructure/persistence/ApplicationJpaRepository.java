package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.application.Application;
import com.jobtracker.domain.application.ApplicationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationJpaRepository extends JpaRepository<Application, Long>, ApplicationRepository {
}

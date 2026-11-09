package com.ssangtteongnip.domain.application;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository {
    Application save(Application application);
    Optional<Application> findById(Long id);
    List<Application> findByUserId(Long userId);
    List<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status);
}

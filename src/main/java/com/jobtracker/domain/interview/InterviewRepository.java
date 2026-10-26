package com.jobtracker.domain.interview;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository {
    Interview save(Interview interview);
    Optional<Interview> findById(Long id);
    List<Interview> findByApplicationId(Long applicationId);
}

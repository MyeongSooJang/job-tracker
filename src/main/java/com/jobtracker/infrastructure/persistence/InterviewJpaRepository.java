package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.interview.Interview;
import com.jobtracker.domain.interview.InterviewRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewJpaRepository extends JpaRepository<Interview, Long>, InterviewRepository {
    List<Interview> findByApplicationId(Long applicationId);
}

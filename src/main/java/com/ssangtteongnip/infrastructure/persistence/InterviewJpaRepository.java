package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.interview.Interview;
import com.ssangtteongnip.domain.interview.InterviewRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewJpaRepository extends JpaRepository<Interview, Long>, InterviewRepository {
    List<Interview> findByApplicationId(Long applicationId);
}

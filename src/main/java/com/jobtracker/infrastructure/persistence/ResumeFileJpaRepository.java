package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.profile.ResumeFile;
import com.jobtracker.domain.profile.ResumeFileRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeFileJpaRepository extends JpaRepository<ResumeFile, Long>, ResumeFileRepository {
}

package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.profile.ResumeFile;
import com.ssangtteongnip.domain.profile.ResumeFileRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeFileJpaRepository extends JpaRepository<ResumeFile, Long>, ResumeFileRepository {
}

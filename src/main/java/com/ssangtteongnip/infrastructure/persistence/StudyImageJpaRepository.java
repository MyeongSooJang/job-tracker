package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.image.StudyImage;
import com.ssangtteongnip.domain.image.StudyImageRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyImageJpaRepository extends JpaRepository<StudyImage, Long>, StudyImageRepository {
}

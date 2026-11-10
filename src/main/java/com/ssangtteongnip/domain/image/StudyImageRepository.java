package com.ssangtteongnip.domain.image;

import java.util.Optional;
import java.util.UUID;

public interface StudyImageRepository {
    StudyImage save(StudyImage image);
    void delete(StudyImage image);
    Optional<StudyImage> findByPublicId(UUID publicId);
}

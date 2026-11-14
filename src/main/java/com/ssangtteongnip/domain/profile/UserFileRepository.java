package com.ssangtteongnip.domain.profile;

import java.util.List;
import java.util.Optional;

public interface UserFileRepository {
    UserFile save(UserFile file);
    Optional<UserFile> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
    // 업로드 최신순. 업로드 시각이 같으면 나중에 저장된(id가 큰) 파일이 먼저 온다.
    List<UserFile> findUserFilesNewestFirst(Long userId);
    void delete(UserFile file);
}

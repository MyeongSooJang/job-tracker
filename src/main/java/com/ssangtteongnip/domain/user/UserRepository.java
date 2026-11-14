package com.ssangtteongnip.domain.user;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    // 행 잠금(PESSIMISTIC_WRITE). 같은 사용자의 동시 요청을 직렬화할 때 쓴다.
    Optional<User> findByIdForUpdate(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

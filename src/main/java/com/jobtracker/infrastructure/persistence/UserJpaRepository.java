package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.user.User;
import com.jobtracker.domain.user.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {
}

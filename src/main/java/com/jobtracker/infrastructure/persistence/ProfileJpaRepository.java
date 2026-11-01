package com.jobtracker.infrastructure.persistence;

import com.jobtracker.domain.profile.Profile;
import com.jobtracker.domain.profile.ProfileRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileJpaRepository extends JpaRepository<Profile, Long>, ProfileRepository {
}

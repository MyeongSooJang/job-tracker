package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.profile.Profile;
import com.ssangtteongnip.domain.profile.ProfileRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileJpaRepository extends JpaRepository<Profile, Long>, ProfileRepository {
}

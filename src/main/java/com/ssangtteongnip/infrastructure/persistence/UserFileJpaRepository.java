package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.profile.UserFile;
import com.ssangtteongnip.domain.profile.UserFileRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFileJpaRepository extends JpaRepository<UserFile, Long>, UserFileRepository {

    @Override
    @Query("select f from UserFile f where f.user.id = :userId order by f.createdAt desc, f.id desc")
    List<UserFile> findUserFilesNewestFirst(@Param("userId") Long userId);
}
